package com.travelmate.repository;

import com.travelmate.entity.Booking;
import com.travelmate.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    // ── Admin queries ──────────────────────────────────────────────────────────

    /** Lấy tất cả booking kèm user + accommodation. Dùng cho trang admin/bookings. */
    @Query("""
            SELECT b FROM Booking b
            JOIN FETCH b.user
            JOIN FETCH b.accommodation
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findAllWithUserAndAccommodation();

    /** Đếm booking theo trạng thái toàn hệ thống. Dùng cho admin dashboard. */
    long countByBookingStatus(BookingStatus bookingStatus);


    /** Đếm tổng booking của 1 user. Dùng cho admin/users để hiển thị số booking. */
    long countByUserId(Long userId);

    // ── Availability / Double-booking prevention ───────────────────────────────

    /**
     * Kiểm tra xem accommodation đã có booking nào trùng ngày chưa.
     *
     * LOGIC OVERLAP (chuẩn interval math):
     *   Hai khoảng [A_in, A_out) và [B_in, B_out) bị chồng nhau khi:
     *   A_in < B_out AND A_out > B_in
     *
     * Chỉ tính booking PENDING và CONFIRMED (CANCELLED và COMPLETED không block).
     * excludeBookingId dùng khi sửa booking — bỏ qua chính booking đó.
     *
     * @param accommodationId  ID accommodation cần kiểm tra
     * @param checkIn          Ngày nhận phòng yêu cầu
     * @param checkOut         Ngày trả phòng yêu cầu
     * @param excludeBookingId Bỏ qua booking này (dùng -1L nếu không cần loại trừ)
     * @return true nếu có conflict
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.accommodation.id = :accommodationId
              AND b.bookingStatus IN ('PENDING', 'CONFIRMED')
              AND b.id <> :excludeBookingId
              AND b.checkIn  < :checkOut
              AND b.checkOut > :checkIn
            """)
    boolean hasOverlappingBooking(
            @Param("accommodationId")  Long      accommodationId,
            @Param("checkIn")          LocalDate checkIn,
            @Param("checkOut")         LocalDate checkOut,
            @Param("excludeBookingId") Long      excludeBookingId
    );

    /**
     * Lấy danh sách các khoảng ngày đang bận của 1 accommodation.
     * Dùng cho UI: disable / highlight ngày đã có booking trên date-picker.
     *
     * Chỉ trả về booking PENDING và CONFIRMED (không CANCELLED / COMPLETED).
     * Trả về Object[] = {checkIn, checkOut} để mapping thủ công (tránh DTO phức tạp).
     */
    @Query("""
            SELECT b.checkIn, b.checkOut FROM Booking b
            WHERE b.accommodation.id = :accommodationId
              AND b.bookingStatus IN ('PENDING', 'CONFIRMED')
              AND b.checkOut >= CURRENT_DATE
            ORDER BY b.checkIn ASC
            """)
    List<Object[]> findBookedDateRanges(@Param("accommodationId") Long accommodationId);
}
