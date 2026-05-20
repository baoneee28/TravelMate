package com.travelmate.scheduler;

import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BookingExpiryScheduler — Tự động hủy booking PENDING_PAYMENT đã quá hạn.
 *
 * Chạy mỗi phút, quét tất cả booking PENDING_PAYMENT có expireAt < now.
 * Với mỗi booking hết hạn:
 *   1. Payment → EXPIRED
 *   2. Booking → CANCELLED
 *   3. Trả lại quota phòng
 *
 * Đảm bảo phòng không bị giữ mãi khi user abandon trang VNPAY.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    /**
     * Chạy mỗi 60 giây.
     * fixedDelay = 60_000ms đảm bảo khoảng cách giữa 2 lần chạy (không overlap).
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelExpiredPendingPaymentBookings() {
        List<com.travelmate.entity.Booking> expiredBookings =
                bookingRepository.findExpiredPendingPaymentBookings(
                        BookingStatus.PENDING_PAYMENT, LocalDateTime.now());

        if (expiredBookings.isEmpty()) return;

        log.info("BookingExpiryScheduler: Tìm thấy {} booking hết hạn thanh toán",
                 expiredBookings.size());

        for (com.travelmate.entity.Booking booking : expiredBookings) {
            try {
                // Tìm payment tương ứng
                paymentRepository.findByBooking(booking).ifPresentOrElse(
                    payment -> {
                        if (payment.getPaymentStatus() == PaymentStatus.PENDING_PAYMENT) {
                            paymentService.expirePayment(payment);
                        }
                    },
                    () -> {
                        // Không có payment → chỉ hủy booking
                        booking.setBookingStatus(BookingStatus.CANCELLED);
                        booking.setPaymentStatus(PaymentStatus.EXPIRED);
                        booking.setNote("Tự động hủy: quá hạn thanh toán VNPAY.");
                        bookingRepository.save(booking);
                        log.warn("Booking {} không có payment, chỉ hủy booking", booking.getBookingCode());
                    }
                );
            } catch (Exception e) {
                log.error("Lỗi khi hủy booking {} hết hạn: {}", booking.getBookingCode(), e.getMessage());
            }
        }
    }
}
