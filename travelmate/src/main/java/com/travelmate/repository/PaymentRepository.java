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

    /**
     * Tìm payment theo mã tham chiếu VNPAY (vnp_TxnRef).
     * Dùng trong IPN / Return URL để tra cứu giao dịch.
     */
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);

    /** Lấy tất cả payment theo trạng thái */
    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);

    /** Lấy payment theo nhiều trạng thái — APPROVED + DEPOSIT_FORFEITED = doanh thu thật */
    List<Payment> findByPaymentStatusIn(List<PaymentStatus> statuses);

    /**
     * Lấy payment của accommodation thuộc về 1 partner (owner).
     * Dùng cho Revenue và Settlement của Partner.
     */
    List<Payment> findByBookingAccommodationOwnerAndPaymentStatusIn(
            User owner, List<PaymentStatus> statuses);
}

