package com.travelmate.repository;

import com.travelmate.entity.Booking;
import com.travelmate.entity.Payment;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PaymentRepository — Giao tiếp với bảng `payments`.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Tìm payment theo booking */
    Optional<Payment> findByBooking(Booking booking);

    /** Lấy tất cả payment theo trạng thái — dùng tính doanh thu demo */
    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);

    /** Lấy payment theo nhiều trạng thái — APPROVED + DEPOSIT_FORFEITED = doanh thu thật */
    List<Payment> findByPaymentStatusIn(List<PaymentStatus> statuses);

    /**
     * Lấy payment của accommodation thuộc về 1 partner (owner).
     * Dùng cho Revenue và Settlement của Partner.
     * Spring Data JPA tự sinh: JOIN booking JOIN accommodation WHERE accommodation.owner = :owner
     */
    List<Payment> findByBookingAccommodationOwnerAndPaymentStatusIn(
            User owner, List<PaymentStatus> statuses);
}

