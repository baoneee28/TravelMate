package com.travelmate.entity.enums;

/**
 * PartnerBookingStatus — Trạng thái xác nhận từ phía partner.
 *
 * Luồng:
 *   Admin duyệt booking → partnerStatus = PENDING_PARTNER_CONFIRMATION
 *   Partner xác nhận giữ phòng → partnerStatus = PARTNER_CONFIRMED
 *
 * Các trạng thái PARTNER_COMPLETED, PARTNER_CANCELLED
 * được chuẩn bị sẵn cho phase sau, chưa dùng trong task này.
 */
public enum PartnerBookingStatus {
    PENDING_PARTNER_CONFIRMATION,  // Chờ partner xác nhận giữ phòng
    PARTNER_CONFIRMED,             // Partner đã xác nhận giữ phòng
    PARTNER_COMPLETED,             // (Dự phòng) Partner xác nhận hoàn tất
    PARTNER_CANCELLED              // (Dự phòng) Partner hủy
}
