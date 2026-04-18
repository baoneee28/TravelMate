package com.travelmate.repository;

import com.travelmate.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository thao tác với bảng "reviews" trong MySQL.
 *
 * Các query chính:
 * - Kiểm tra booking đã được review chưa
 * - Lấy danh sách review của 1 accommodation
 * - Lấy danh sách booking_id đã review của 1 user (để hiển thị nút trên UI)
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Kiểm tra booking đã được review chưa.
     * Dùng để validate: mỗi booking chỉ review 1 lần.
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Lấy tất cả review của 1 accommodation, mới nhất trước.
     * Dùng JOIN FETCH để load user info (tên người review).
     */
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.user
            WHERE r.accommodation.id = :accommodationId
            ORDER BY r.createdAt DESC
            """)
    List<Review> findByAccommodationIdWithUser(@Param("accommodationId") Long accommodationId);

    /**
     * Lấy danh sách booking ID đã review của 1 user.
     * Dùng để đánh dấu "Đã đánh giá" trên trang mybooking.
     */
    @Query("SELECT r.booking.id FROM Review r WHERE r.user.id = :userId")
    List<Long> findReviewedBookingIdsByUserId(@Param("userId") Long userId);

    // ── Admin queries ──────────────────────────────────────────────────────────

    /** Lấy tất cả review kèm user + accommodation. Dùng cho admin/reviews. */
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.user
            JOIN FETCH r.accommodation
            ORDER BY r.createdAt DESC
            """)
    List<Review> findAllWithUserAndAccommodation();
}
