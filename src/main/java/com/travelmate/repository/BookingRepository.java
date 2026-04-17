package com.travelmate.repository;

import com.travelmate.entity.Booking;
import com.travelmate.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác với bảng "bookings" trong MySQL.
 *
 * Spring Data JPA tự tạo implementation khi app khởi động.
 * Chỉ cần khai báo method signature, Spring tự generate query.
 *
 * Các method query tùy chỉnh dùng JPQL (viết theo tên class/field Java,
 * không phải tên bảng/cột SQL).
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Lấy tất cả booking của 1 user, sắp xếp mới nhất trước.
     *
     * JOIN FETCH: khi load booking sẽ load luôn accommodation
     * trong 1 query duy nhất (tránh N+1 problem).
     *
     * N+1 problem là gì?
     * - Nếu không dùng JOIN FETCH, khi lấy 10 booking:
     *   → 1 query lấy 10 booking
     *   → 10 query riêng lẻ lấy 10 accommodation tương ứng
     *   → Tổng: 11 query (chậm!)
     * - Dùng JOIN FETCH: chỉ 1 query duy nhất, gộp cả booking + accommodation
     *
     * @param userId ID của user đang đăng nhập
     * @return Danh sách booking kèm thông tin accommodation, mới nhất trước
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.accommodation
            WHERE b.user.id = :userId
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findByUserIdWithAccommodation(@Param("userId") Long userId);

    /**
     * Tìm booking theo ID có kèm thông tin accommodation.
     *
     * Dùng JOIN FETCH tương tự: 1 query duy nhất lấy cả booking + accommodation.
     * Cần khi xem chi tiết hoặc hủy booking.
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.accommodation
            WHERE b.id = :bookingId
            """)
    Optional<Booking> findByIdWithAccommodation(@Param("bookingId") Long bookingId);

    /**
     * Tìm booking theo ID và user ID.
     *
     * BẢO MẬT: đảm bảo user chỉ thao tác được booking của chính mình.
     * Ví dụ: user A không thể hủy booking của user B.
     *
     * @param bookingId ID booking
     * @param userId    ID user đang đăng nhập
     * @return Optional chứa booking nếu tìm thấy và thuộc về user đó
     */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.accommodation
            WHERE b.id = :bookingId AND b.user.id = :userId
            """)
    Optional<Booking> findByIdAndUserId(
            @Param("bookingId") Long bookingId,
            @Param("userId") Long userId
    );

    /**
     * Đếm số booking theo trạng thái của 1 user.
     * Dùng cho dashboard: "Bạn có 3 booking đang chờ xác nhận".
     */
    long countByUserIdAndBookingStatus(Long userId, BookingStatus bookingStatus);
}
