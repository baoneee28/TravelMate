package com.travelmate.enums;

/**
 * Enum dai dien cho trang thai cua mot don dat cho (booking).
 *
 * Luong hoat dong:
 * 1. User dat cho -> PENDING
 * 2. He thong xac nhan -> CONFIRMED
 * 3. User huy -> CANCELLED
 * 4. User hoan tat ky nghi -> COMPLETED
 *
 * Chi booking co trang thai COMPLETED moi duoc phep review.
 */
public enum BookingStatus {

    PENDING,    // Dang cho xac nhan
    CONFIRMED,  // Da xac nhan
    CANCELLED,  // Da huy
    COMPLETED   // Da hoan tat
}
