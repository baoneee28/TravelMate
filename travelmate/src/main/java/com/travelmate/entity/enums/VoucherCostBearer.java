package com.travelmate.entity.enums;

/**
 * VoucherCostBearer — Ai chịu chi phí giảm giá từ voucher.
 *
 * ADMIN:   Admin chịu chi phí → không trừ vào settlement của partner
 * PARTNER: Partner chịu chi phí → trừ vào settlement của partner
 */
public enum VoucherCostBearer {
    ADMIN,   // Admin chịu — voucher USER_GLOBAL do admin tạo
    PARTNER  // Partner chịu — voucher PARTNER_ROOM/ACCOMMODATION do partner tạo
}
