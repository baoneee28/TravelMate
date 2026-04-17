package com.travelmate.dto.response;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO truyền thông tin review từ Service → Controller → Template.
 *
 * Chứa thông tin review + tên người đánh giá.
 * Dùng cho:
 * - Hiển thị danh sách review trên trang chi tiết accommodation
 * - Response sau khi tạo review thành công
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    /** ID review */
    private Long id;

    /** ID booking tương ứng */
    private Long bookingId;

    /** ID accommodation */
    private Long accommodationId;

    /** Tên nơi lưu trú */
    private String accommodationName;

    /** Tên người đánh giá */
    private String userName;

    /** Điểm đánh giá (1-5) */
    private Integer rating;

    /** Nhận xét */
    private String comment;

    /** Thời gian tạo review */
    private LocalDateTime createdAt;
}
