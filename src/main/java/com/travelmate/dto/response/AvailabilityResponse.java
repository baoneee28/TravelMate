package com.travelmate.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO trả về kết quả kiểm tra tình trạng phòng trống.
 *
 * Dùng cho endpoint: GET /api/bookings/availability?accommodationId=X&checkIn=Y&checkOut=Z
 *
 * Client (JavaScript) dùng response này để:
 * 1. Hiển thị cảnh báo ngay khi user chọn ngày đã bận
 * 2. Disable nút "Thanh toán" nếu ngày bị trùng
 * 3. Hiển thị danh sách ngày đã bận trên date-picker (bookedRanges)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {

    /**
     * true = phòng còn trống trong khoảng ngày yêu cầu.
     * false = đã có booking trùng ngày.
     */
    private boolean available;

    /**
     * Thông báo lỗi khi available = false.
     * Ví dụ: "Nơi lưu trú đã được đặt từ 15/05 đến 18/05. Vui lòng chọn ngày khác."
     * null khi available = true.
     */
    private String conflictMessage;

    /**
     * Danh sách các khoảng ngày đang bận (chỉ PENDING và CONFIRMED).
     * Trả về ngay cả khi không cần kiểm tra checkIn/checkOut,
     * để UI dùng highlight / block ngày trên date-picker.
     */
    private List<BookedRange> bookedRanges;

    /**
     * Bản ghi đơn giản cho 1 khoảng ngày đã bận.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookedRange {
        private LocalDate checkIn;
        private LocalDate checkOut;
    }
}
