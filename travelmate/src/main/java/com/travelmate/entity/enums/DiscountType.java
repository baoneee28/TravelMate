package com.travelmate.entity.enums;

/**
 * DiscountType — Loại giảm giá của voucher.
 * PERCENT:       giảm theo % tổng tiền
 * FIXED_AMOUNT:  giảm một số tiền cố định
 */
public enum DiscountType {
    PERCENT,        // Giảm % (VD: 10%)
    FIXED_AMOUNT    // Giảm tiền cố định (VD: 50.000đ)
}
