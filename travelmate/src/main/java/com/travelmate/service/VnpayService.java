package com.travelmate.service;

import com.travelmate.config.VnpayConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * VnpayService — Xử lý tạo URL thanh toán và xác minh chữ ký VNPAY.
 *
 * Tài liệu tham khảo: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
 *
 * Lưu ý kỹ thuật VNPAY:
 *   - vnp_Amount gửi theo đơn vị VND × 100 (ví dụ 100.000đ → 10000000)
 *   - Các tham số phải sort theo key trước khi hash
 *   - Hash function: HMAC SHA512 với hashSecret
 *   - Encoding: URL encode theo chuẩn US-ASCII
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VnpayService {

    private static final DateTimeFormatter VNPAY_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayConfigProperties config;

    /**
     * Tạo URL thanh toán redirect sang VNPAY Sandbox.
     *
     * @param vnpTxnRef  Mã giao dịch duy nhất (vnp_TxnRef)
     * @param amountVnd  Số tiền VND (CHƯA nhân 100)
     * @param orderInfo  Thông tin đơn hàng (vnp_OrderInfo)
     * @param ipAddr     IP của user
     * @param expireAt   Thời điểm hết hạn giao dịch
     * @return URL thanh toán đầy đủ (đã kèm vnp_SecureHash)
     */
    public String createPaymentUrl(String vnpTxnRef, long amountVnd,
                                   String orderInfo, String ipAddr,
                                   LocalDateTime expireAt) {

        // Tất cả params phải sort theo key — dùng TreeMap để tự động sort
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version",    config.getVersion());
        params.put("vnp_Command",    config.getCommand());
        params.put("vnp_TmnCode",    config.getTmnCode());
        params.put("vnp_Amount",     String.valueOf(amountVnd * 100L));
        params.put("vnp_CurrCode",   "VND");
        params.put("vnp_TxnRef",     vnpTxnRef);
        params.put("vnp_OrderInfo",  sanitizeOrderInfo(orderInfo));
        params.put("vnp_OrderType",  config.getOrderType());
        params.put("vnp_Locale",     "vn");
        params.put("vnp_ReturnUrl",  config.getReturnUrl());
        params.put("vnp_IpAddr",     ipAddr);
        params.put("vnp_CreateDate", LocalDateTime.now().format(VNPAY_DATE_FMT));
        params.put("vnp_ExpireDate", expireAt.format(VNPAY_DATE_FMT));

        // Build hash data và query string — cả hai đều dùng URL-encoded values (chuẩn VNPAY Java sample)
        StringBuilder hashData = new StringBuilder();
        StringBuilder queryString = new StringBuilder();

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                hashData.append('&');
                queryString.append('&');
            }
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII);
            hashData.append(entry.getKey()).append('=').append(encodedValue);
            queryString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                       .append('=')
                       .append(encodedValue);
            first = false;
        }

        String secureHash = hmacSHA512(config.getHashSecret(), hashData.toString());
        queryString.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = config.getPayUrl() + "?" + queryString;
        log.debug("VNPAY Payment URL created for txnRef={}", vnpTxnRef);
        return paymentUrl;
    }

    /**
     * Xác minh chữ ký HMAC SHA512 từ VNPAY trả về (Return URL hoặc IPN).
     *
     * Quy trình:
     *   1. Lấy vnp_SecureHash ra khỏi map
     *   2. Sort các params còn lại theo key
     *   3. Hash lại bằng hashSecret
     *   4. So sánh (case-insensitive)
     *
     * @param params Toàn bộ query params từ VNPAY (bao gồm vnp_SecureHash)
     * @return true nếu chữ ký hợp lệ
     */
    public boolean verifySignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            log.warn("VNPAY: vnp_SecureHash không có trong request");
            return false;
        }

        // Loại bỏ các field liên quan đến hash
        Map<String, String> filteredParams = new TreeMap<>(params);
        filteredParams.remove("vnp_SecureHash");
        filteredParams.remove("vnp_SecureHashType");

        // Build hash data theo thứ tự key đã sort
        StringBuilder hashData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : filteredParams.entrySet()) {
            if (!first) hashData.append('&');
            hashData.append(entry.getKey()).append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            first = false;
        }

        String computedHash = hmacSHA512(config.getHashSecret(), hashData.toString());
        boolean valid = computedHash.equalsIgnoreCase(receivedHash);

        if (!valid) {
            log.warn("VNPAY: Chữ ký không hợp lệ. Expected={}, Received={}",
                     computedHash, receivedHash);
        }
        return valid;
    }

    /**
     * Kiểm tra số tiền VNPAY trả về có khớp với số tiền giao dịch không.
     * VNPAY trả về vnp_Amount = amountVnd × 100
     *
     * @param vnpAmountStr Giá trị vnp_Amount từ VNPAY
     * @param expectedVnd  Số tiền thực tế (VND, chưa nhân 100)
     * @return true nếu khớp
     */
    public boolean validateAmount(String vnpAmountStr, long expectedVnd) {
        try {
            long received = Long.parseLong(vnpAmountStr);
            return received == expectedVnd * 100L;
        } catch (NumberFormatException e) {
            log.warn("VNPAY: vnp_Amount không hợp lệ: {}", vnpAmountStr);
            return false;
        }
    }

    // ===== PRIVATE HELPERS =====

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac.init(secretKey);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi HMAC SHA512: " + e.getMessage(), e);
        }
    }

    /** Loại bỏ ký tự đặc biệt trong order info (VNPAY yêu cầu ASCII) */
    private String sanitizeOrderInfo(String orderInfo) {
        if (orderInfo == null) return "TravelMate Booking";
        // Giữ lại chữ, số, khoảng trắng, gạch ngang — loại bỏ ký tự đặc biệt
        return orderInfo.replaceAll("[^a-zA-Z0-9 \\-_]", "").trim();
    }
}
