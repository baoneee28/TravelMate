package com.travelmate.entity.enums;

/**
 * PaymentOption — Hình thức thanh toán do user chọn.
 *
 * 2 lựa chọn:
 *   - FULL_PAYMENT: thanh toán 100% tổng tiền
 *   - DEPOSIT_30:   chỉ cọc 30%, còn lại thanh toán sau
 */
public enum PaymentOption {
    FULL_PAYMENT,  // Thanh toán 100%
    DEPOSIT_30     // Cọc 30%
}
