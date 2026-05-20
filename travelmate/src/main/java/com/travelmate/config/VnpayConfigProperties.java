package com.travelmate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VnpayConfigProperties — Đọc config VNPAY từ application.properties.
 *
 * Sử dụng prefix "vnpay", ví dụ:
 *   vnpay.tmn-code=XXXXXXXX
 *   vnpay.hash-secret=XXXXXXXXXXXXXXXXXXXXXXXX
 *   vnpay.return-url=http://localhost:8080/payment/vnpay-return
 *   vnpay.ipn-url=https://xxx.ngrok-free.app/payment/vnpay-ipn
 */
@Component
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnpayConfigProperties {

    /** URL cổng thanh toán VNPAY Sandbox */
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    /** Mã website trên hệ thống VNPAY (TmnCode) */
    private String tmnCode;

    /** Khóa bí mật để ký HMAC SHA512 */
    private String hashSecret;

    /** Return URL — VNPAY redirect user về sau khi thanh toán xong */
    private String returnUrl;

    /** IPN URL — VNPAY gọi server-to-server để xác nhận giao dịch */
    private String ipnUrl;

    /** Phiên bản API VNPAY */
    private String version = "2.1.0";

    /** Lệnh: "pay" */
    private String command = "pay";

    /** Loại đơn hàng */
    private String orderType = "other";

    /** Số phút trước khi giao dịch hết hạn */
    private int expireMinutes = 15;
}
