package com.travelmate.entity.enums;

/**
 * BookingStatus — Trạng thái vòng đời đơn đặt phòng.
 *
 * Luồng chuẩn (VNPAY):
 *   User tạo yêu cầu   → PENDING_PAYMENT
 *   VNPAY thành công    → PENDING_ADMIN_APPROVAL
 *   Admin xác nhận      → CONFIRMED
 *   Partner check-in    → CHECKED_IN
 *   Partner check-out   → COMPLETED
 *
 *   User hủy/VNPAY lỗi  → CANCELLED
 *   Cọc 30% no-show     → NO_SHOW
 */
public enum BookingStatus {
    PENDING_PAYMENT,         // User đang chuyển đến cổng thanh toán VNPAY (phòng đã giữ tạm)
    PENDING_ADMIN_APPROVAL,  // VNPAY đã xác nhận thanh toán, chờ TravelMate xác nhận
    CONFIRMED,               // Admin đã xác nhận, chuyển cho partner giữ phòng
    CHECKED_IN,              // Khách đã nhận phòng
    NO_SHOW,                 // Khách cọc 30% không đến — mất cọc
    CANCELLED,               // Đã hủy (VNPAY lỗi / user hủy / admin từ chối / quá hạn)
    COMPLETED                // Hoàn tất sau khi check-out
}
