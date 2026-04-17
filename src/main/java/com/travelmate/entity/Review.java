package com.travelmate.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entity Review - ánh xạ vào bảng "reviews" trong MySQL.
 *
 * Đây là entity đại diện cho đánh giá của USER sau khi hoàn tất booking:
 * - Chỉ booking có trạng thái COMPLETED mới được review
 * - Mỗi booking chỉ được review 1 lần (unique constraint)
 * - Review gồm: rating (1-5 sao) + comment (nhận xét)
 *
 * Kế thừa BaseEntity để có sẵn: id, createdAt, updatedAt
 */
@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_review_user",          columnList = "user_id"),
        @Index(name = "idx_review_accommodation", columnList = "accommodation_id"),
        @Index(name = "idx_review_booking",        columnList = "booking_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_booking", columnNames = "booking_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    /**
     * USER đã viết review này.
     * - @ManyToOne: nhiều review có thể thuộc 1 user
     * - FetchType.LAZY: chỉ load user khi cần
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Nơi lưu trú được review.
     * - @ManyToOne: 1 accommodation có thể có nhiều review
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    /**
     * Booking tương ứng (mỗi booking chỉ review 1 lần).
     * - @OneToOne: 1 booking → 1 review duy nhất
     * - Unique constraint ở @Table đảm bảo không trùng
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /**
     * Điểm đánh giá: 1 → 5 sao.
     */
    @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa 5 sao")
    @Column(nullable = false)
    private Integer rating;

    /**
     * Nhận xét của khách.
     * Không được để trống, tối đa 1000 ký tự.
     */
    @NotBlank(message = "Vui lòng nhập nhận xét")
    @Size(max = 1000, message = "Nhận xét tối đa 1000 ký tự")
    @Column(nullable = false, length = 1000)
    private String comment;
}
