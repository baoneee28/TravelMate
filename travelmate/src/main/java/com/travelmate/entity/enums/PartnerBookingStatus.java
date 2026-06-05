package com.travelmate.entity.enums;

/**
 * PartnerBookingStatus — Trạng thái xác nhận từ phía đối tác.
 *
 * Luồng:
 *   VNPAY xác minh thành công → partnerStatus = PARTNER_CONFIRMED
 *   Đối tác check-out/hoàn tất → partnerStatus = PARTNER_COMPLETED
 *   Đối tác báo không thể tiếp nhận khách → partnerStatus = PARTNER_CANCELLED
 */
public enum PartnerBookingStatus {
    PENDING_PARTNER_CONFIRMATION,  // Trạng thái legacy/ngoại lệ cần kiểm tra
    PARTNER_CONFIRMED,             // TravelMate đã giữ phòng/căn
    PARTNER_COMPLETED,             // Đối tác xác nhận hoàn tất lưu trú
    PARTNER_CANCELLED              // Đối tác báo không thể tiếp nhận khách
}
