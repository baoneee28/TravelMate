package com.travelmate.entity.enums;

/**
 * BookingSource — Nguồn gốc tạo booking.
 *
 * ONLINE       : User đặt qua TravelMate platform (luồng chính).
 * DIRECT       : Partner tạo thủ công cho khách tới trực tiếp tại cơ sở.
 * MANUAL_BLOCK : Partner chặn phòng/căn (bảo trì, giữ nội bộ, v.v.)
 *
 * Chỉ ONLINE booking đi vào settlement và tính commission.
 * DIRECT và MANUAL_BLOCK chỉ ảnh hưởng availability quota TravelMate.
 */
public enum BookingSource {
    ONLINE,         // Đặt qua TravelMate
    DIRECT,         // Booking trực tiếp tại cơ sở (partner tạo)
    MANUAL_BLOCK    // Chặn phòng/căn (bảo trì, giữ nội bộ)
}
