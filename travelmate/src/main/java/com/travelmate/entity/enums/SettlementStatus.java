package com.travelmate.entity.enums;

/**
 * SettlementStatus — Trạng thái quyết toán cho partner.
 *
 * PENDING:   Admin đã tạo settlement, chưa ghi nhận chi trả cho partner
 * PAID:      Admin đã ghi nhận chi trả cho partner ngoài hệ thống
 * CANCELLED: Settlement bị hủy (ít dùng, để backup)
 */
public enum SettlementStatus {
    PENDING,    // Chờ ghi nhận chi trả
    PAID,       // Đã ghi nhận chi trả
    CANCELLED   // Đã hủy
}
