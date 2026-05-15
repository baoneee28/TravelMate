package com.travelmate.entity.enums;

/**
 * BookingStatus — Trạng thái đơn đặt phòng.
 *
 * Luồng chính:
 *   User đặt phòng   → PENDING_ADMIN_APPROVAL
 *   Admin duyệt      → CONFIRMED
 *   Admin no-show    → NO_SHOW (cọc 30%, khách mất cọc)
 *                   hoặc CHECKED_IN (100%, hệ thống xử lý check-in)
 *   User hủy         → CANCELLED
 *   Hoàn tất         → COMPLETED
 */
public enum BookingStatus {
    PENDING_ADMIN_APPROVAL,  // Chờ admin duyệt (mặc định khi user vừa đặt)
    CONFIRMED,               // Admin đã duyệt, xác nhận đặt phòng
    CHECKED_IN,              // Khách đã check-in (hoặc 100% + no-show → vẫn xử lý check-in)
    NO_SHOW,                 // Khách cọc 30% không đến → mất cọc
    CANCELLED,               // Đã hủy (user hoặc admin hủy)
    COMPLETED                // Đã hoàn tất (sau khi checkout)
}
