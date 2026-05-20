package com.travelmate.controller.page;

import com.travelmate.entity.Booking;
import com.travelmate.entity.Payment;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.security.CustomUserDetails;
import com.travelmate.service.PaymentService;
import com.travelmate.service.TravelPostService;
import com.travelmate.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * PaymentController — Điều khiển luồng thanh toán VNPAY.
 *
 * Routes:
 *   GET /payment/vnpay/create/{bookingId}  → Tạo URL và redirect sang VNPAY
 *   GET /payment/vnpay-return              → Xử lý Return URL → hiển thị trang payment-result
 *   GET /payment/vnpay-ipn                 → Xử lý IPN (server-to-server, cập nhật DB)
 *
 * Phân biệt Return URL vs IPN:
 *   - Return URL: User-facing, verify checksum + cập nhật DB (fallback) → redirect /my-bookings.
 *   - IPN:        Server-to-server. VNPAY gọi trực tiếp vào server qua ngrok/Cloudflare Tunnel.
 *                 Đây là nguồn cập nhật DB CHÍNH (chắc chắn hơn Return URL).
 *
 * Option 2 (được chọn): IPN cập nhật DB chính, Return URL fallback + redirect về /my-bookings.
 * Để dùng IPN thật: cần chạy ngrok trỏ về localhost:8080, config vnpay.ipn-url trong application.properties.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class PaymentController {
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final TravelPostService travelPostService;
    private final VnpayService vnpayService;

    // ===========================
    // 1. TẠO VNPAY PAYMENT URL
    // ===========================

    /**
     * Tạo URL thanh toán VNPAY và redirect người dùng sang cổng.
     *
     * GET /payment/vnpay/create/{bookingId}
     *
     * Yêu cầu:
     *   - User phải đang đăng nhập và là chủ booking
     *   - Booking phải ở trạng thái PENDING_PAYMENT
     *   - Payment phải có vnpTxnRef (được set khi tạo booking)
     */
    @GetMapping("/payment/vnpay/create/{bookingId}")
    public String createVnpayPayment(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (currentUser == null) return "redirect:/auth/login";

        Optional<Booking> optBooking = bookingRepository.findById(java.util.Objects.requireNonNull(bookingId));
        if (optBooking.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đơn đặt phòng!");
            return "redirect:/my-bookings";
        }

        Booking booking = optBooking.get();

        // Kiểm tra quyền sở hữu
        if (!booking.getUser().getEmail().equals(currentUser.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền thực hiện thanh toán này!");
            return "redirect:/my-bookings";
        }

        // Chỉ được thanh toán khi đang PENDING_PAYMENT
        if (booking.getBookingStatus() != BookingStatus.PENDING_PAYMENT) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đơn đặt phòng này không ở trạng thái chờ thanh toán!");
            return "redirect:/my-bookings";
        }

        Optional<Payment> optPayment = paymentRepository.findByBooking(booking);
        if (optPayment.isEmpty() || optPayment.get().getVnpTxnRef() == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không tìm thấy thông tin thanh toán. Vui lòng thử lại!");
            return "redirect:/my-bookings";
        }

        Payment payment = optPayment.get();

        // Kiểm tra đã hết hạn chưa (expireAt được set khi tạo booking)
        if (booking.getExpireAt() != null &&
            booking.getExpireAt().isBefore(java.time.LocalDateTime.now())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Phiên đặt phòng đã hết hạn (quá 15 phút). Vui lòng đặt phòng lại!");
            return "redirect:/my-bookings";
        }

        // Lấy IP client (xử lý proxy/ngrok)
        String ipAddr = extractClientIp(request);

        // Mô tả đơn hàng
        String orderInfo = "TravelMate Booking " + booking.getBookingCode()
                + " tai " + booking.getAccommodation().getName();

        // Tạo URL VNPAY
        String paymentUrl = vnpayService.createPaymentUrl(
                payment.getVnpTxnRef(),
                payment.getAmount().longValue(),
                orderInfo,
                ipAddr,
                booking.getExpireAt()
        );

        log.info("Redirect user sang VNPAY: bookingId={}, vnpTxnRef={}", bookingId, payment.getVnpTxnRef());
        return "redirect:" + paymentUrl;
    }

    // ===========================
    // 2. VNPAY RETURN URL
    // ===========================

    /**
     * Xử lý Return URL — VNPAY redirect user về sau khi thanh toán xong.
     *
     * GET /payment/vnpay-return (permitAll — không cần session)
     *
     * Option 2: Return URL chỉ đọc DB (đã được IPN cập nhật) và hiển thị kết quả.
     * Nếu IPN chưa gọi kịp: Return URL cũng gọi markGatewaySuccess/Failed để đảm bảo.
     */
    @GetMapping("/payment/vnpay-return")
    public String vnpayReturn(
            @RequestParam Map<String, String> params,
            org.springframework.ui.Model model,
            RedirectAttributes redirectAttributes) {

        log.info("VNPAY Return URL nhận được: txnRef={}, responseCode={}",
                 params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"));

        String vnpTxnRef         = params.get("vnp_TxnRef");
        String responseCode      = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        // 1. Xác minh chữ ký — quan trọng nhất, chống giả mạo
        if (!vnpayService.verifySignature(params)) {
            log.warn("VNPAY Return: Chữ ký không hợp lệ! txnRef={}", vnpTxnRef);
            model.addAttribute("isSuccess", false);
            model.addAttribute("message", "Không thể xác thực kết quả thanh toán VNPAY. Vui lòng liên hệ TravelMate để được hỗ trợ.");
            model.addAttribute("txnRef", vnpTxnRef);
            model.addAttribute("errorCode", "INVALID_SIGNATURE");
            return "user/payment-result";
        }

        // VNPAY yêu cầu CẢ HAI responseCode VÀ transactionStatus đều phải là "00"
        boolean isSuccess = "00".equals(responseCode) && "00".equals(transactionStatus);

        if (isSuccess) {
            // Xác minh số tiền
            Optional<Payment> optPay = paymentRepository.findByVnpTxnRef(vnpTxnRef);
            if (optPay.isPresent()) {
                String vnpAmount = params.get("vnp_Amount");
                if (!vnpayService.validateAmount(vnpAmount, optPay.get().getAmount().longValue())) {
                    log.warn("VNPAY Return: Số tiền không khớp! txnRef={}", vnpTxnRef);
                    model.addAttribute("isSuccess", false);
                    model.addAttribute("message", "Số tiền thanh toán không khớp. Vui lòng liên hệ TravelMate để được hỗ trợ.");
                    model.addAttribute("txnRef", vnpTxnRef);
                    model.addAttribute("errorCode", "AMOUNT_MISMATCH");
                    return "user/payment-result";
                }
            }

            // Gọi service (idempotent — nếu IPN đã xử lý rồi thì bỏ qua)
            paymentService.markGatewaySuccess(vnpTxnRef, params);

            // Đọc lại booking để hiển thị thông tin chi tiết
            Optional<Payment> pay = paymentRepository.findByVnpTxnRef(vnpTxnRef);
            Booking booking = pay.map(Payment::getBooking).orElse(null);

            String paymentType = "";
            String msg;
            if (booking != null
                    && booking.getPaymentOption() != null
                    && "DEPOSIT_30".equals(booking.getPaymentOption().name())) {
                paymentType = "DEPOSIT_30";
                msg = "Thanh toán cọc 30% qua VNPAY thành công! Đơn đặt phòng đang chờ TravelMate xác nhận.";
            } else {
                paymentType = "FULL_PAYMENT";
                msg = "Thanh toán 100% qua VNPAY thành công! Đơn đặt phòng đang chờ TravelMate xác nhận.";
            }

            model.addAttribute("isSuccess", true);
            model.addAttribute("message", msg);
            model.addAttribute("txnRef", vnpTxnRef);
            model.addAttribute("paymentType", paymentType);

            if (booking != null) {
                model.addAttribute("bookingCode", booking.getBookingCode());
                model.addAttribute("accName", booking.getAccommodation() != null
                        ? booking.getAccommodation().getName() : "—");
                if (booking.getAccommodation() != null) {
                    String city = booking.getAccommodation().getCity();
                    model.addAttribute("travelDestination", city);
                    model.addAttribute("travelDestinationLabel", travelPostService.getDestinationDisplayName(city));
                    model.addAttribute("travelDestinationSlug", travelPostService.normalizeDestination(city));
                    model.addAttribute("checkIn", booking.getCheckIn() != null ? booking.getCheckIn().toString() : "");
                    model.addAttribute("checkOut", booking.getCheckOut() != null ? booking.getCheckOut().toString() : "");
                    model.addAttribute("checkInFormatted", formatDateVN(booking.getCheckIn()));
                    model.addAttribute("checkOutFormatted", formatDateVN(booking.getCheckOut()));
                    model.addAttribute("adults", booking.getAdults() != null ? booking.getAdults() : 1);
                    model.addAttribute("children", booking.getChildren() != null ? booking.getChildren() : 0);
                    model.addAttribute("rooms", booking.getRoomQuantity() != null ? booking.getRoomQuantity() : 1);
                }
            }
            if (pay.isPresent()) {
                java.text.NumberFormat nf = java.text.NumberFormat.getInstance(Locale.of("vi", "VN"));
                model.addAttribute("amount", nf.format(pay.get().getAmount()) + "đ");
            }

            return "user/payment-result";

        } else {
            // Xử lý thất bại (idempotent)
            paymentService.markGatewayFailed(vnpTxnRef, responseCode, params);

            String msg = switch (responseCode) {
                case "24" -> "Bạn đã hủy giao dịch VNPAY. Đơn đặt phòng đã được hủy và phòng đã được mở lại.";
                case "11" -> "Giao dịch VNPAY đã hết hạn. Vui lòng đặt phòng lại.";
                case "09" -> "Thẻ/Tài khoản không đủ số dư. Vui lòng kiểm tra và thử lại.";
                case "10" -> "Xác thực thẻ không thành công quá 3 lần. Tài khoản tạm thời bị khóa.";
                default   -> "Thanh toán VNPAY không thành công (mã: " + responseCode + "). Vui lòng thử lại.";
            };

            model.addAttribute("isSuccess", false);
            model.addAttribute("message", msg);
            model.addAttribute("txnRef", vnpTxnRef);
            model.addAttribute("errorCode", responseCode);
            return "user/payment-result";
        }
    }

    // ===========================
    // 3. VNPAY IPN (SERVER-TO-SERVER)
    // ===========================

    /**
     * IPN URL — VNPAY gọi server-to-server để xác nhận giao dịch.
     *
     * GET /payment/vnpay-ipn (permitAll)
     *
     * Đây là nguồn cập nhật DB CHÍNH trong Option 2.
     * Cần ngrok/Cloudflare Tunnel để VNPAY gọi được vào localhost.
     *
     * Response format theo tài liệu VNPAY:
     *   {"RspCode":"00","Message":"Confirm Success"}
     */
    @GetMapping("/payment/vnpay-ipn")
    @ResponseBody
    public ResponseEntity<Map<String, String>> vnpayIpn(
            @RequestParam Map<String, String> params) {

        log.info("VNPAY IPN nhận được: txnRef={}, responseCode={}, amount={}",
                 params.get("vnp_TxnRef"), params.get("vnp_ResponseCode"), params.get("vnp_Amount"));

        String vnpTxnRef    = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String vnpAmountStr = params.get("vnp_Amount");

        // 1. Xác minh chữ ký
        if (!vnpayService.verifySignature(params)) {
            log.warn("VNPAY IPN: Chữ ký không hợp lệ! txnRef={}", vnpTxnRef);
            return ResponseEntity.ok(ipnResponse("97", "Invalid Signature"));
        }

        // 2. Tìm payment theo vnpTxnRef
        Optional<Payment> optPayment = paymentRepository.findByVnpTxnRef(vnpTxnRef);
        if (optPayment.isEmpty()) {
            log.warn("VNPAY IPN: Không tìm thấy payment với txnRef={}", vnpTxnRef);
            return ResponseEntity.ok(ipnResponse("01", "Order Not Found"));
        }

        Payment payment = optPayment.get();

        // 3. Idempotency — đã xử lý rồi thì báo OK nhưng không xử lý lại
        if (payment.getPaymentStatus() != PaymentStatus.PENDING_PAYMENT) {
            log.info("VNPAY IPN: Payment {} đã xử lý ({})", vnpTxnRef, payment.getPaymentStatus());
            return ResponseEntity.ok(ipnResponse("02", "Order Already Confirmed"));
        }

        // 4. Xác minh số tiền (bảo vệ chống giả mạo amount)
        if (!vnpayService.validateAmount(vnpAmountStr, payment.getAmount().longValue())) {
            log.warn("VNPAY IPN: Số tiền không khớp! txnRef={}, expected={}×100, received={}",
                     vnpTxnRef, payment.getAmount().longValue(), vnpAmountStr);
            // Lưu raw IPN payload để audit
            payment.setRawIpnPayload(buildPayload(params));
            paymentRepository.save(payment);
            return ResponseEntity.ok(ipnResponse("04", "Invalid Amount"));
        }

        // 5. Xử lý kết quả giao dịch — cả responseCode VÀ transactionStatus phải là "00"
        if ("00".equals(responseCode) && "00".equals(params.get("vnp_TransactionStatus"))) {
            // Lưu raw IPN trước khi gọi service
            payment.setRawIpnPayload(buildPayload(params));
            paymentRepository.save(payment);
            paymentService.markGatewaySuccess(vnpTxnRef, params);
            log.info("VNPAY IPN: txnRef={} → THÀNH CÔNG ✓", vnpTxnRef);
        } else {
            payment.setRawIpnPayload(buildPayload(params));
            paymentRepository.save(payment);
            paymentService.markGatewayFailed(vnpTxnRef, responseCode, params);
            log.info("VNPAY IPN: txnRef={} → THẤT BẠI (code={})", vnpTxnRef, responseCode);
        }

        return ResponseEntity.ok(ipnResponse("00", "Confirm Success"));
    }

    // ===========================
    // PRIVATE HELPERS
    // ===========================

    private Map<String, String> ipnResponse(String rspCode, String message) {
        Map<String, String> resp = new LinkedHashMap<>();
        resp.put("RspCode", rspCode);
        resp.put("Message", message);
        return resp;
    }

    private String buildPayload(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlankOrUnknown(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (isBlankOrUnknown(ip)) ip = request.getHeader("WL-Proxy-Client-IP");
        if (isBlankOrUnknown(ip)) ip = request.getRemoteAddr();
        // Lấy IP đầu tiên nếu có nhiều (X-Forwarded-For có thể chứa chuỗi IPs)
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return (ip != null && !ip.isBlank()) ? ip : "127.0.0.1";
    }

    private boolean isBlankOrUnknown(String s) {
        return s == null || s.isBlank() || "unknown".equalsIgnoreCase(s);
    }

    private String formatDateVN(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE_FORMATTER);
    }
}
