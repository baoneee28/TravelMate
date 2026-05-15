package com.travelmate.entity.enums;

/**
 * PaymentMethod — Phương thức thanh toán.
 *
 * Hiện tại chỉ dùng VNPAY_DEMO (mô phỏng).
 * Sau này có thể thêm MOMO_DEMO, BANK_TRANSFER, v.v.
 */
public enum PaymentMethod {
    VNPAY_DEMO,    // VNPay mô phỏng (chưa tích hợp thật)
    MOMO_DEMO      // MoMo mô phỏng (dự phòng cho sau này)
}
