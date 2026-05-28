package com.travelmate.entity.enums;

/**
 * VoucherScope — Phạm vi áp dụng voucher.
 *
 * USER_GLOBAL:            Admin tạo, tự động áp dụng được cho mọi phòng.
 * PARTNER_ACCOMMODATION:  Giữ tương thích dữ liệu cũ, không tạo mới từ UI.
 * PARTNER_ROOM:           Admin đưa vào kho, partner gắn vào phòng/căn đã duyệt.
 */
public enum VoucherScope {
    USER_GLOBAL,
    PARTNER_ACCOMMODATION,
    PARTNER_ROOM
}
