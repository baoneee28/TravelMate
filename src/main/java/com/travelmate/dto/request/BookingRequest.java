package com.travelmate.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * DTO nhận dữ liệu từ form đặt phòng (booking.html).
 *
 * Khi user nhấn "Thanh toán" trên trang booking:
 * 1. Form HTML gửi POST request đến /api/bookings
 * 2. Spring tự động bind dữ liệu form vào BookingRequest
 * 3. @Valid trong controller sẽ kiểm tra các annotation (NotNull, Min...)
 * 4. Nếu hợp lệ → tạo Booking entity
 * 5. Nếu không hợp lệ → trả về lỗi validation
 *
 * Tại sao dùng DTO thay vì nhận Booking entity trực tiếp?
 * - An toàn: user không thể tự set id, status, totalPrice
 * - Chỉ nhận đúng data cần thiết từ form
 * - Tách biệt request data và business logic
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    /**
     * ID của accommodation (nơi lưu trú) cần đặt.
     * Lấy từ trang chi tiết accommodation → truyền qua form.
     */
    @NotNull(message = "Vui lòng chọn nơi lưu trú")
    private Long accommodationId;

    /**
     * Ngày nhận phòng.
     * Format: yyyy-MM-dd (HTML5 date input tự format).
     */
    @NotNull(message = "Vui lòng chọn ngày nhận phòng")
    private LocalDate checkIn;

    /**
     * Ngày trả phòng.
     * Phải sau ngày nhận phòng (validate ở Service layer).
     */
    @NotNull(message = "Vui lòng chọn ngày trả phòng")
    private LocalDate checkOut;

    /**
     * Số người lớn. Mặc định 1.
     */
    @Min(value = 1, message = "Số người lớn phải >= 1")
    private Integer numAdults = 1;

    /**
     * Số trẻ em. Mặc định 0.
     */
    @Min(value = 0, message = "Số trẻ em phải >= 0")
    private Integer numChildren = 0;

    /**
     * Ghi chú của khách (tuỳ chọn, không bắt buộc).
     */
    private String notes;
}
