package com.travelmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Review — Entity ánh xạ bảng `reviews` trong MySQL.
 *
 * Đại diện cho 1 đánh giá của user sau khi booking COMPLETED.
 *
 * Business rules:
 *   - 1 booking chỉ được review 1 lần (unique constraint trên booking_id)
 *   - Booking phải ở trạng thái COMPLETED mới được review
 *   - User phải là người sở hữu booking
 *   - Rating: 1-5 sao
 *
 * Quan hệ:
 *   - ManyToOne → User (người đánh giá)
 *   - ManyToOne → Accommodation (nơi lưu trú được đánh giá)
 *   - OneToOne → Booking (booking đã hoàn tất)
 */
@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Người viết đánh giá */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Nơi lưu trú được đánh giá */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    /**
     * Booking đã COMPLETED liên kết với review.
     * Mỗi booking chỉ được review 1 lần.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /** Điểm đánh giá 1-5 sao */
    @Column(nullable = false)
    private Integer rating;

    /** Nội dung đánh giá */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * Admin có thể ẩn đánh giá vi phạm mà không xóa vật lý.
     * isHidden = true → user không thấy, admin vẫn thấy với badge "Đã ẩn"
     */
    @Column(name = "is_hidden", nullable = false)
    private Boolean isHidden = false;

    /** Thời gian tạo đánh giá */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
