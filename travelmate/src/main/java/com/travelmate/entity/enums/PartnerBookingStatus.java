package com.travelmate.entity.enums;

/**
 * PartnerBookingStatus — Trạng thái xác nhận từ phía đối tác.
 *
 * Luồng:
 *   VNPAY xác minh thành công → partnerStatus = PENDING_PARTNER_CONFIRMATION
 *   Đối tác xác nhận giữ phòng → partnerStatus = PARTNER_CONFIRMED
 *
 * Các trạng thái PARTNER_COMPLETED, PARTNER_CANCELLED
 * được chuẩn bị sẵn cho phase sau, chưa dùng trong task này.
 */
public enum PartnerBookingStatus {
    PENDING_PARTNER_CONFIRMATION,  // Chờ đối tác xác nhận giữ phòng
    PARTNER_CONFIRMED,             // Đối tác đã xác nhận giữ phòng
    PARTNER_COMPLETED,             // (Dự phòng) Đối tác xác nhận hoàn tất
    PARTNER_CANCELLED              // (Dự phòng) Đối tác hủy
}
