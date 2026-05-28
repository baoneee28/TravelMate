package com.travelmate.entity.enums;

/**
 * VoucherCostBearer — Ai chịu chi phí giảm giá từ voucher.
 *
 * ADMIN:   Admin chịu chi phí → không trừ vào settlement của partner
 * PARTNER: Partner chịu chi phí khi chọn gắn voucher → trừ vào settlement
 */
public enum VoucherCostBearer {
    ADMIN,
    PARTNER
}
