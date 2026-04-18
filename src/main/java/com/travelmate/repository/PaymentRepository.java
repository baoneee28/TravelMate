package com.travelmate.repository;

import com.travelmate.entity.Payment;
import com.travelmate.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * PaymentRepository — thao tác với bảng payments.
 *
 * Spring Data JPA tự tạo SQL từ tên method (không cần viết SQL tay).
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Tìm payment theo booking ID.
     *
     * Dùng khi:
     * - Hiển thị chi tiết thanh toán trên trang "Đặt phòng của tôi"
     * - Admin xem / xác nhận thanh toán
     *
     * @param bookingId ID của booking
     * @return Optional<Payment> — empty nếu chưa có payment (không nên xảy ra)
     */
    Optional<Payment> findByBookingId(Long bookingId);

    /**
     * Tìm payment theo booking ID và trạng thái.
     *
     * Ví dụ: tìm payment chưa thanh toán của booking #7
     *   findByBookingIdAndPaymentStatus(7L, PaymentStatus.UNPAID)
     *
     * @param bookingId     ID booking
     * @param paymentStatus trạng thái cần tìm
     */
    Optional<Payment> findByBookingIdAndPaymentStatus(Long bookingId, PaymentStatus paymentStatus);

    /**
     * Kiểm tra payment đã tồn tại chưa theo booking ID.
     *
     * Dùng để tránh tạo trùng payment cho cùng 1 booking.
     *
     * @param bookingId ID booking
     * @return true nếu đã có payment
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Lấy payment kèm booking (JOIN FETCH) — tránh N+1 query.
     *
     * @param bookingId ID booking
     */
    @Query("SELECT p FROM Payment p JOIN FETCH p.booking WHERE p.booking.id = :bookingId")
    Optional<Payment> findByBookingIdWithBooking(@Param("bookingId") Long bookingId);
}
