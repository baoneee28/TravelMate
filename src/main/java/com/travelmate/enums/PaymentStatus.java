package com.travelmate.enums;

/**
 * Enum dai dien cho trang thai thanh toan.
 *
 * - UNPAID: chua thanh toan (mac dinh khi tao booking)
 * - PAID: da thanh toan thanh cong
 * - FAILED: thanh toan that bai (loi cong thanh toan, het thoi gian...)
 * - REFUNDED: da hoan tien (khi user huy booking hop le)
 */
public enum PaymentStatus {

    UNPAID,     // Chua thanh toan
    PAID,       // Da thanh toan thanh cong
    FAILED,     // Thanh toan that bai
    REFUNDED    // Da hoan tien
}
