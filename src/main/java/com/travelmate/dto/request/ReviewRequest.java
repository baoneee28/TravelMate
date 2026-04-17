package com.travelmate.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO nhận dữ liệu từ form đánh giá (review modal trên trang mybooking).
 *
 * Khi user nhấn "Gửi đánh giá":
 * 1. JavaScript gửi POST request đến /api/reviews
 * 2. Spring tự động bind JSON vào ReviewRequest
 * 3. @Valid kiểm tra rating (1-5) và comment (không trống)
 * 4. Nếu hợp lệ → tạo Review entity
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequest {

    /**
     * ID booking cần review.
     * Booking phải có trạng thái COMPLETED và thuộc về user đang đăng nhập.
     */
    @NotNull(message = "Vui lòng chọn đặt phòng cần đánh giá")
    private Long bookingId;

    /**
     * Điểm đánh giá: 1 → 5 sao.
     */
    @NotNull(message = "Vui lòng chọn số sao")
    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    private Integer rating;

    /**
     * Nhận xét của khách.
     */
    @NotBlank(message = "Vui lòng nhập nhận xét")
    @Size(max = 1000, message = "Nhận xét tối đa 1000 ký tự")
    private String comment;
}
