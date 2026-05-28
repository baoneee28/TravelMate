package com.travelmate.service;

import com.travelmate.entity.Booking;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PartnerBookingStatus;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * PaymentService — Xử lý kết quả thanh toán từ cổng VNPAY.
 *
 * Được gọi từ:
 *   - PaymentController.vnpayReturn() — khi VNPAY redirect user về
 *   - PaymentController.vnpayIpn()    — khi VNPAY gọi IPN server-to-server
 *
 * Idempotent: mọi method đều kiểm tra trạng thái hiện tại trước khi xử lý.
 * Nếu payment đã được xử lý rồi → bỏ qua, không cộng doanh thu hoặc trừ quota 2 lần.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    /**
     * Xử lý khi VNPAY báo thanh toán THÀNH CÔNG.
     *
     * Chỉ xử lý nếu payment đang ở PENDING_PAYMENT (idempotent).
     * Sau khi xử lý:
     *   - paymentStatus = APPROVED vì kết quả VNPAY đã được xác minh chữ ký và số tiền
     *   - bookingStatus = CONFIRMED
     *   - partnerStatus = PENDING_PARTNER_CONFIRMATION
     *
     * @param vnpTxnRef  Mã giao dịch (vnp_TxnRef)
     * @param vnpParams  Toàn bộ params từ VNPAY (để lưu audit)
     * @return true nếu xử lý thành công hoặc đã xử lý trước đó
     */
    @Transactional
    public boolean markGatewaySuccess(String vnpTxnRef, Map<String, String> vnpParams) {
        Optional<Payment> optPayment = paymentRepository.findByVnpTxnRef(vnpTxnRef);
        if (optPayment.isEmpty()) {
            log.warn("PaymentService.markGatewaySuccess: Không tìm thấy payment với vnpTxnRef={}", vnpTxnRef);
            return false;
        }

        Payment payment = optPayment.get();

        // Idempotency: đã xử lý rồi → trả về true (không xử lý lại)
        if (payment.getPaymentStatus() != PaymentStatus.PENDING_PAYMENT) {
            log.info("PaymentService.markGatewaySuccess: Payment {} đã ở trạng thái {} — bỏ qua",
                     vnpTxnRef, payment.getPaymentStatus());
            return true;
        }

        // Cập nhật thông tin giao dịch từ VNPAY
        payment.setVnpTransactionNo(vnpParams.get("vnp_TransactionNo"));
        payment.setVnpBankCode(vnpParams.get("vnp_BankCode"));
        payment.setVnpBankTranNo(vnpParams.get("vnp_BankTranNo"));
        payment.setVnpCardType(vnpParams.get("vnp_CardType"));
        payment.setVnpResponseCode(vnpParams.get("vnp_ResponseCode"));
        payment.setVnpTransactionStatus(vnpParams.get("vnp_TransactionStatus"));
        payment.setVnpPayDate(vnpParams.get("vnp_PayDate"));
        payment.setConfirmedFromGatewayAt(LocalDateTime.now());
        payment.setRawReturnPayload(buildPayload(vnpParams));
        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setApprovedAt(LocalDateTime.now());
        payment.setNote("VNPAY xác nhận thành công; TravelMate tự động ghi nhận thanh toán.");
        paymentRepository.save(payment);

        // Cập nhật booking
        Booking booking = payment.getBooking();
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPartnerStatus(PartnerBookingStatus.PENDING_PARTNER_CONFIRMATION);
        booking.setExpireAt(null); // Đã thanh toán xong, không cần expireAt nữa
        bookingRepository.save(booking);

        // Thanh toán đã được hệ thống xác nhận; partner vẫn phải nhận giữ phòng/căn.
        try {
            String optionLabel = (payment.getPaymentOption() != null
                    && payment.getPaymentOption().name().equals("DEPOSIT_30"))
                    ? "khoản cọc 30%"
                    : "thanh toán 100%";
            notificationService.createPaymentReceived(
                    booking.getUser(),
                    booking.getBookingCode(),
                    booking.getAccommodation(),
                    optionLabel);
        } catch (Exception e) {
            log.warn("Không gửi được notification cho booking {}: {}", booking.getId(), e.getMessage());
        }

        log.info("PaymentService.markGatewaySuccess: vnpTxnRef={} → CONFIRMED / APPROVED, chờ đối tác xác nhận", vnpTxnRef);
        return true;
    }

    /**
     * Xử lý khi VNPAY báo thanh toán THẤT BẠI / HỦY / HẾT HẠN.
     *
     * Chỉ xử lý nếu payment đang ở PENDING_PAYMENT (idempotent).
     * Sau khi xử lý:
     *   - paymentStatus = FAILED / CANCELLED / EXPIRED
     *   - bookingStatus = CANCELLED
     *   - Trả lại quota phòng cho room
     *
     * Mã VNPAY phổ biến:
     *   24 → User hủy giao dịch → CANCELLED
     *   11 → Giao dịch hết hạn  → EXPIRED
     *   Khác → Lỗi hệ thống     → FAILED
     *
     * @param vnpTxnRef    Mã giao dịch
     * @param responseCode Mã phản hồi từ VNPAY (vnp_ResponseCode)
     * @param vnpParams    Toàn bộ params (để lưu audit)
     */
    @Transactional
    public void markGatewayFailed(String vnpTxnRef, String responseCode,
                                  Map<String, String> vnpParams) {
        Optional<Payment> optPayment = paymentRepository.findByVnpTxnRef(vnpTxnRef);
        if (optPayment.isEmpty()) {
            log.warn("PaymentService.markGatewayFailed: Không tìm thấy payment với vnpTxnRef={}", vnpTxnRef);
            return;
        }

        Payment payment = optPayment.get();

        // Idempotency
        if (payment.getPaymentStatus() != PaymentStatus.PENDING_PAYMENT) {
            log.info("PaymentService.markGatewayFailed: Payment {} đã ở trạng thái {} — bỏ qua",
                     vnpTxnRef, payment.getPaymentStatus());
            return;
        }

        // Xác định trạng thái lỗi
        PaymentStatus failedStatus = switch (responseCode) {
            case "24" -> PaymentStatus.CANCELLED;
            case "11" -> PaymentStatus.EXPIRED;
            default   -> PaymentStatus.FAILED;
        };

        payment.setVnpResponseCode(responseCode);
        payment.setPaymentStatus(failedStatus);
        payment.setRawReturnPayload(buildPayload(vnpParams));
        paymentRepository.save(payment);

        // Hủy booking
        Booking booking = payment.getBooking();
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(failedStatus);
        booking.setExpireAt(null);
        bookingRepository.save(booking);

        // Trả lại quota phòng (phòng đã được giữ tạm khi tạo PENDING_PAYMENT)
        Room room = booking.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
        roomRepository.save(room);

        log.info("PaymentService.markGatewayFailed: vnpTxnRef={} → {} (code={})",
                 vnpTxnRef, failedStatus, responseCode);
    }

    /**
     * Hủy booking PENDING_PAYMENT đã hết hạn (gọi từ Scheduler).
     * Trả lại quota phòng và set trạng thái EXPIRED.
     */
    @Transactional
    public void expirePayment(Payment payment) {
        if (payment.getPaymentStatus() != PaymentStatus.PENDING_PAYMENT) return;

        payment.setPaymentStatus(PaymentStatus.EXPIRED);
        payment.setExpireAt(null);
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.EXPIRED);
        booking.setNote("Tự động hủy: quá 3 phút chưa hoàn tất thanh toán VNPAY.");
        booking.setExpireAt(null);
        bookingRepository.save(booking);

        // Trả lại quota
        Room room = booking.getRoom();
        room.setAvailableQuantity(room.getAvailableQuantity() + booking.getRoomQuantity());
        roomRepository.save(room);

        log.info("Booking {} hết hạn thanh toán → CANCELLED (EXPIRED)", booking.getBookingCode());
    }

    // ===== PRIVATE HELPERS =====

    private String buildPayload(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> sb.append(k).append("=").append(v).append("&"));
        return sb.toString();
    }
}
