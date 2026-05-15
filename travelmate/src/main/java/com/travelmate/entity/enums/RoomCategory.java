package com.travelmate.entity.enums;

/**
 * RoomCategory — Phân loại phòng trong một cơ sở lưu trú.
 *
 * Mục đích:
 *   - Phân biệt loại phòng để áp dụng commission riêng (commissionRateOverride).
 *   - Demo rõ ràng: STANDARD dùng rate mặc định, VIP/SUITE có thể rate cao hơn.
 *
 * Mapping tiếng Việt (dùng trong DTO/UI):
 *   STANDARD → Phòng tiêu chuẩn
 *   DELUXE   → Phòng Deluxe
 *   FAMILY   → Phòng gia đình
 *   VIP      → Phòng VIP
 *   SUITE    → Phòng Suite
 *   OTHER    → Khác
 */
public enum RoomCategory {
    STANDARD,
    DELUXE,
    FAMILY,
    VIP,
    SUITE,
    OTHER
}
