package com.travelmate.entity.enums;

/**
 * PaymentStatus — Trạng thái thanh toán.
 *
 * Luồng VNPAY:
 *   User bấm thanh toán → PENDING_PAYMENT
 *   VNPAY xác nhận OK   → PENDING_ADMIN_APPROVAL
 *   Admin xác nhận      → APPROVED
 *   VNPAY lỗi           → FAILED
 *   User hủy trên VNPAY → CANCELLED
 *   Quá 15 phút         → EXPIRED
 *   Cọc 30% + no-show   → DEPOSIT_FORFEITED
 *   Hoàn tiền           → REFUND_PENDING → REFUNDED
 */
public enum PaymentStatus {
    PENDING_PAYMENT,        // User chưa hoàn tất thanh toán (đang trên cổng VNPAY)
    PENDING_ADMIN_APPROVAL, // VNPAY xác nhận thành công, chờ TravelMate/Admin xác nhận
    APPROVED,               // Admin đã xác nhận thanh toán hợp lệ
    REJECTED,               // Admin từ chối (trường hợp đặc biệt)
    FAILED,                 // VNPAY báo giao dịch thất bại
    CANCELLED,              // User hủy giao dịch trên cổng VNPAY (mã 24)
    EXPIRED,                // Giao dịch VNPAY hết hạn (mã 11) hoặc quá 15 phút chưa thanh toán
    DEPOSIT_FORFEITED,      // Cọc 30% bị giữ lại vì khách không đến check-in
    REFUND_PENDING,         // Chờ admin xác nhận hoàn tiền
    REFUNDED,               // Đã hoàn tiền cho user
    NOT_REQUIRED,           // Không áp dụng thanh toán TravelMate (DIRECT / MANUAL_BLOCK)
    SUBMITTED               // Legacy — giữ để không lỗi dữ liệu DB cũ
}
