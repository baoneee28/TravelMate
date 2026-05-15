package com.travelmate.entity.enums;

/**
 * PaymentStatus — Trạng thái thanh toán.
 *
 * Luồng đầy đủ:
 *   User thanh toán demo   → PENDING_ADMIN_APPROVAL
 *   Admin duyệt            → APPROVED
 *   Admin từ chối          → REJECTED
 *   User hoặc admin hủy   → CANCELLED
 *   Cọc 30% + no-show      → DEPOSIT_FORFEITED (mất cọc)
 */
public enum PaymentStatus {
    SUBMITTED,                // (legacy) User đã xác nhận thanh toán — giữ lại để không lỗi DB cũ
    PENDING_ADMIN_APPROVAL,   // Chờ admin xác nhận thanh toán
    APPROVED,                 // Admin đã xác nhận → thanh toán thành công
    REJECTED,                 // Admin từ chối thanh toán (payment demo không hợp lệ)
    CANCELLED,                // Booking bị hủy → payment không còn hiệu lực
    DEPOSIT_FORFEITED,        // Cọc 30% bị giữ lại vì khách không đến check-in
    REFUND_PENDING,           // Đang chờ hoàn tiền (partner hủy sau khi user đã thanh toán)
    REFUNDED                  // Đã hoàn tiền cho user thành công
}
