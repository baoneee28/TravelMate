package com.travelmate.repository;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Review;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ReviewRepository — Giao tiếp với bảng `reviews`.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** Lấy reviews của 1 accommodation, mới nhất trước — hiển thị trên trang detail */
    List<Review> findByAccommodationOrderByCreatedAtDesc(Accommodation accommodation);

    /** Lấy reviews của 1 user — trang profile (nếu cần) */
    List<Review> findByUserOrderByCreatedAtDesc(User user);

    /** Kiểm tra booking đã được review chưa — 1 booking = 1 review max */
    boolean existsByBooking(Booking booking);

    /** Tìm review theo booking */
    Optional<Review> findByBooking(Booking booking);

    /** Đếm review của 1 accommodation — tính reviewCount */
    long countByAccommodation(Accommodation accommodation);

    /** Lấy tất cả reviews, mới nhất trước — admin quản lý */
    List<Review> findAllByOrderByCreatedAtDesc();

    /** Kiểm tra booking đã review chưa bằng booking ID — tiện cho bulk check */
    boolean existsByBookingId(Long bookingId);

    /** Lấy reviews của nhiều accommodation — dùng cho partner xem tất cả review cơ sở mình */
    List<Review> findByAccommodationInOrderByCreatedAtDesc(List<Accommodation> accommodations);

    @Query("""
            select avg(r.rating)
            from Review r
            where r.booking.room = :room
              and r.isHidden = false
              and r.createdAt >= :from
              and r.createdAt < :to
            """)
    Double findAverageRatingByRoomAndCreatedAtBetweenAndIsHiddenFalse(@Param("room") Room room,
                                                                      @Param("from") LocalDateTime from,
                                                                      @Param("to") LocalDateTime to);

    long countByBooking_RoomAndCreatedAtBetweenAndIsHiddenFalse(Room room,
                                                                LocalDateTime from,
                                                                LocalDateTime to);
}
