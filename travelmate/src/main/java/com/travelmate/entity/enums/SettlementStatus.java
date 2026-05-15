package com.travelmate.entity.enums;

/**
 * SettlementStatus — Trạng thái quyết toán cho partner.
 *
 * PENDING:   Admin đã tạo settlement, chưa thanh toán cho partner
 * PAID:      Admin đã thanh toán cho partner
 * CANCELLED: Settlement bị hủy (ít dùng, để backup)
 */
public enum SettlementStatus {
    PENDING,    // Chờ thanh toán
    PAID,       // Đã thanh toán
    CANCELLED   // Đã hủy
}
