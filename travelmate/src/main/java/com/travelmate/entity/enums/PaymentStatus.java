package com.travelmate.entity.enums;

/**
 * PaymentStatus — Trạng thái thanh toán.
 *
 * Luồng VNPAY:
 *   User bấm thanh toán → PENDING_PAYMENT
 *   VNPAY xác nhận OK   → APPROVED (hệ thống tự động ghi nhận)
 *   VNPAY lỗi           → FAILED
 *   User hủy trên VNPAY → CANCELLED
 *   Quá 3 phút          → EXPIRED
 *   Cọc 30% + hủy/no-show → DEPOSIT_FORFEITED
 *   Hoàn tiền           → REFUND_PENDING → REFUNDED
 */
public enum PaymentStatus {
    PENDING_PAYMENT,        // User chưa hoàn tất thanh toán (đang trên cổng VNPAY)
    PENDING_ADMIN_APPROVAL, // Legacy/ngoại lệ: cần Admin đối soát thủ công
    APPROVED,               // VNPAY đã xác minh thành công hoặc Admin xử lý ngoại lệ
    REJECTED,               // Admin từ chối (trường hợp đặc biệt)
    FAILED,                 // VNPAY báo giao dịch thất bại
    CANCELLED,              // User hủy giao dịch trên cổng VNPAY (mã 24)
    EXPIRED,                // Giao dịch VNPAY hết hạn (mã 11) hoặc quá 3 phút chưa thanh toán
    DEPOSIT_FORFEITED,      // Cọc 30% bị giữ lại khi khách hủy hoặc không đến check-in
    REFUND_PENDING,         // Chờ admin xác nhận hoàn tiền
    REFUNDED,               // Đã hoàn tiền cho user
    NOT_REQUIRED,           // Không áp dụng thanh toán TravelMate (DIRECT / MANUAL_BLOCK)
    SUBMITTED               // Legacy — giữ để không lỗi dữ liệu DB cũ
}
