package com.travelmate.repository;

import com.travelmate.entity.Booking;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * BookingRepository — Giao tiếp với bảng `bookings`.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Lấy danh sách booking của 1 user, sắp xếp mới nhất trước */
    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Đếm số booking đã tạo cho 1 loại phòng.
     * Dùng để sinh bookingSequence: BK-R101-<count+1>
     */
    long countByRoom(Room room);

    /** Lấy tất cả booking, mới nhất trước — dùng cho admin. */
    List<Booking> findAllByOrderByCreatedAtDesc();

    /**
     * Lấy tất cả booking cho trang admin với JOIN FETCH đầy đủ.
     * Dùng thay cho findAllByOrderByCreatedAtDesc() để:
     *   1. Tránh LazyInitializationException khi render template
     *   2. Tránh N+1 queries (1 booking = 1 query user + 1 query accommodation + 1 query room)
     *   3. Tránh NullPointerException khi template truy cập b.accommodation.name, b.room.roomName
     */
    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.user
        LEFT JOIN FETCH b.accommodation a
        LEFT JOIN FETCH a.owner
        LEFT JOIN FETCH b.room
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findAllForAdminPage();

    /** Đếm booking theo trạng thái — dùng cho admin dashboard */
    long countByBookingStatus(BookingStatus status);

    /**
     * Lấy booking thuộc accommodation do partner sở hữu.
     * Spring Data JPA tự sinh query: JOIN accommodation WHERE accommodation.owner = :owner
     */
    List<Booking> findByAccommodationOwnerOrderByCreatedAtDesc(User owner);

    /**
     * Lấy TẤT CẢ booking thuộc partner (kể cả DIRECT, MANUAL_BLOCK, PENDING) — dùng cho partner quản lý vận hành.
     */
    @Query("SELECT b FROM Booking b WHERE b.accommodation.owner = :owner ORDER BY b.createdAt DESC")
    List<Booking> findAllByAccommodationOwnerOrderByCreatedAtDesc(@Param("owner") User owner);

    /**
     * Lấy booking thuộc accommodation do partner sở hữu + lọc theo trạng thái.
     * Partner chỉ thấy booking đã được admin xử lý (CONFIRMED, CHECKED_IN, COMPLETED, NO_SHOW).
     * KHÔNG bao gồm PENDING_ADMIN_APPROVAL.
     */
    List<Booking> findByAccommodationOwnerAndBookingStatusInOrderByCreatedAtDesc(
            User owner, List<BookingStatus> statuses);

    /** Lấy booking của 1 accommodation cụ thể, theo trạng thái đang active */
    List<Booking> findByAccommodationAndBookingStatusIn(
            com.travelmate.entity.Accommodation accommodation, List<BookingStatus> statuses);

    // ─── Availability queries ────────────────────────────────────────────────

    /**
     * Tổng số phòng đang bị giữ theo từng room_id (toàn bộ active bookings).
     * Dùng để tính: totalQuantity = room.availableQuantity + sumActiveByRoom.
     * Trả về Object[]{roomId, sumRoomQuantity}.
     */
    @Query("SELECT b.room.id, COALESCE(SUM(b.roomQuantity), 0) FROM Booking b " +
           "WHERE b.bookingStatus IN :statuses GROUP BY b.room.id")
    List<Object[]> sumActiveBookingsByRoom(@Param("statuses") List<BookingStatus> statuses);

    /**
     * Tổng số phòng đang bị giữ theo từng room_id, lọc theo khoảng ngày.
     * Overlap condition: checkIn < requestedCheckOut AND checkOut > requestedCheckIn.
     * Trả về Object[]{roomId, sumRoomQuantity}.
     */
    @Query("SELECT b.room.id, COALESCE(SUM(b.roomQuantity), 0) FROM Booking b " +
           "WHERE b.bookingStatus IN :statuses " +
           "AND b.checkIn < :checkOut AND b.checkOut > :checkIn " +
           "GROUP BY b.room.id")
    List<Object[]> sumOverlappingBookingsByRoom(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("statuses") List<BookingStatus> statuses);

    /**
     * Danh sách booking cụ thể bị overlap trong khoảng ngày — để hiển thị tooltip.
     */
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.bookingStatus IN :statuses " +
           "AND b.checkIn < :checkOut AND b.checkOut > :checkIn " +
           "ORDER BY b.checkIn ASC")
    List<Booking> findOverlappingForRoom(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("statuses") List<BookingStatus> statuses);

    /**
     * Tổng số phòng đang bị giữ (toàn bộ active bookings) cho 1 phòng cụ thể.
     * Dùng để tính totalRooms = room.availableQuantity + sumActiveQtyForRoom.
     * Anti-overbooking: gọi trước khi tạo booking.
     */
    @Query("SELECT COALESCE(SUM(b.roomQuantity), 0) FROM Booking b " +
           "WHERE b.room.id = :roomId AND b.bookingStatus IN :statuses")
    int sumActiveQtyForRoom(@Param("roomId") Long roomId,
                            @Param("statuses") List<BookingStatus> statuses);

    /**
     * Tổng số phòng đang bị giữ trong khoảng ngày yêu cầu cho 1 phòng cụ thể.
     * Anti-overbooking: gọi trước khi tạo booking.
     */
    @Query("SELECT COALESCE(SUM(b.roomQuantity), 0) FROM Booking b " +
           "WHERE b.room.id = :roomId AND b.bookingStatus IN :statuses " +
           "AND b.checkIn < :checkOut AND b.checkOut > :checkIn")
    int sumOverlappingQtyForRoom(@Param("roomId") Long roomId,
                                 @Param("checkIn") LocalDate checkIn,
                                 @Param("checkOut") LocalDate checkOut,
                                 @Param("statuses") List<BookingStatus> statuses);

    /**
     * Tìm tất cả booking PENDING_PAYMENT có expireAt đã qua.
     * Dùng bởi BookingExpiryScheduler để tự động hủy booking quá hạn thanh toán.
     */
    @Query("SELECT b FROM Booking b WHERE b.bookingStatus = :status AND b.expireAt < :now")
    List<Booking> findExpiredPendingPaymentBookings(
            @Param("status") BookingStatus status,
            @Param("now") LocalDateTime now);
}
