package com.travelmate.entity.enums;

/**
 * PropertyType — Loại hình lưu trú.
 *
 * Hiện tại chỉ dùng HOTEL cho task đặt phòng khách sạn.
 * Các loại khác (VILLA, HOMESTAY, RESORT) sẽ dùng khi mở rộng sau.
 */
public enum PropertyType {
    HOTEL,      // Khách sạn
    VILLA,      // Biệt thự nghỉ dưỡng
    HOMESTAY,   // Nhà dân cho thuê
    RESORT      // Khu nghỉ dưỡng
}
