package com.travelmate.entity.enums;

/**
 * VoucherScope — Phạm vi áp dụng voucher.
 *
 * USER_GLOBAL:            Admin tạo, user dùng khi đặt bất kỳ phòng nào
 * PARTNER_ACCOMMODATION:  Partner tạo, gắn vào 1 accommodation cụ thể
 * PARTNER_ROOM:           Partner tạo, gắn vào 1 phòng cụ thể
 */
public enum VoucherScope {
    USER_GLOBAL,            // Admin tạo — áp dụng toàn hệ thống
    PARTNER_ACCOMMODATION,  // Partner tạo — áp dụng cho accommodation của mình
    PARTNER_ROOM            // Partner tạo — áp dụng cho room cụ thể của mình
}
