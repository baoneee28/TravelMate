package com.travelmate.entity.enums;

/**
 * BookingStatus — Trạng thái vòng đời đơn đặt phòng.
 *
 * Luồng chuẩn (VNPAY):
 *   User tạo yêu cầu   → PENDING_PAYMENT
 *   VNPAY thành công    → CONFIRMED + PARTNER_CONFIRMED (TravelMate tự giữ phòng/căn)
 *   Đối tác check-in    → CHECKED_IN
 *   Đối tác check-out   → COMPLETED
 *
 *   User hủy/VNPAY lỗi  → CANCELLED
 *   Cọc 30% no-show     → NO_SHOW
 */
public enum BookingStatus {
    PENDING_PAYMENT,         // User đang chuyển đến cổng thanh toán VNPAY (phòng đã giữ tạm)
    PENDING_ADMIN_APPROVAL,  // Trạng thái legacy/ngoại lệ cần Admin đối soát thủ công
    CONFIRMED,               // VNPAY hợp lệ, TravelMate đã giữ phòng/căn
    CHECKED_IN,              // Khách đã nhận phòng
    NO_SHOW,                 // Khách cọc 30% không đến — mất cọc
    CANCELLED,               // Đã hủy (VNPAY lỗi / user hủy / admin từ chối / quá hạn)
    COMPLETED                // Hoàn tất sau khi check-out
}
