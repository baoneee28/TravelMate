package com.travelmate.service;

import com.travelmate.entity.Booking;
import com.travelmate.entity.Payment;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.repository.PartnerSettlementRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử điều kiện đưa booking vào quyết toán (isSettlementEligible).
 *
 * Nghiệp vụ:
 *   ✓ ONLINE + APPROVED + COMPLETED   → hợp lệ (khách đã hoàn tất lưu trú)
 *   ✓ ONLINE + DEPOSIT_FORFEITED + NO_SHOW → hợp lệ (cọc 30% bị giữ)
 *   ✗ DIRECT hoặc MANUAL_BLOCK        → không quyết toán
 *   ✗ PENDING_ADMIN_APPROVAL           → không quyết toán
 *   ✗ CHECKED_IN, CONFIRMED            → chưa hoàn tất
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementService — Điều kiện quyết toán")
class SettlementEligibilityTest {

    @Mock private PartnerSettlementRepository settlementRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommissionService commissionService;
    @Mock private PartnerWalletService partnerWalletService;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
                settlementRepository, paymentRepository, userRepository, commissionService, partnerWalletService
        );
    }

    private Payment buildPayment(PaymentStatus paymentStatus, BookingStatus bookingStatus, BookingSource source) {
        Booking booking = new Booking();
        booking.setBookingStatus(bookingStatus);
        booking.setBookingSource(source);

        Payment payment = new Payment();
        payment.setPaymentStatus(paymentStatus);
        payment.setBooking(booking);
        return payment;
    }

    private boolean checkEligible(Payment payment) {
        Object result = ReflectionTestUtils.invokeMethod(
                java.util.Objects.requireNonNull(settlementService), "isSettlementEligible", payment
        );
        return Boolean.TRUE.equals(result);
    }

    // ─── Trường hợp HỢP LỆ ───────────────────────────────────────────────────

    @Test
    @DisplayName("✓ ONLINE + APPROVED + COMPLETED → hợp lệ (khách hoàn tất)")
    void eligible_onlineApprovedCompleted() {
        Payment p = buildPayment(PaymentStatus.APPROVED, BookingStatus.COMPLETED, BookingSource.ONLINE);

        assertThat(checkEligible(p)).isTrue();
    }

    @Test
    @DisplayName("✓ ONLINE + DEPOSIT_FORFEITED + NO_SHOW → hợp lệ (mất cọc no-show)")
    void eligible_onlineDepositForfeitedNoShow() {
        Payment p = buildPayment(PaymentStatus.DEPOSIT_FORFEITED, BookingStatus.NO_SHOW, BookingSource.ONLINE);

        assertThat(checkEligible(p)).isTrue();
    }

    // ─── Trường hợp KHÔNG HỢP LỆ ────────────────────────────────────────────

    @Test
    @DisplayName("✗ DIRECT booking — không quyết toán (partner tự tạo)")
    void notEligible_directBooking() {
        Payment p = buildPayment(PaymentStatus.APPROVED, BookingStatus.COMPLETED, BookingSource.DIRECT);

        assertThat(checkEligible(p)).isFalse();
    }

    @Test
    @DisplayName("✗ MANUAL_BLOCK — không quyết toán (admin chặn phòng)")
    void notEligible_manualBlock() {
        Payment p = buildPayment(PaymentStatus.NOT_REQUIRED, BookingStatus.CONFIRMED, BookingSource.MANUAL_BLOCK);

        assertThat(checkEligible(p)).isFalse();
    }

    @Test
    @DisplayName("✗ PENDING_ADMIN_APPROVAL — chưa duyệt, không quyết toán")
    void notEligible_pendingAdminApproval() {
        Payment p = buildPayment(PaymentStatus.PENDING_ADMIN_APPROVAL, BookingStatus.PENDING_ADMIN_APPROVAL, BookingSource.ONLINE);

        assertThat(checkEligible(p)).isFalse();
    }

    @Test
    @DisplayName("✗ ONLINE + APPROVED + CHECKED_IN — chưa hoàn tất, không quyết toán")
    void notEligible_checkedIn() {
        Payment p = buildPayment(PaymentStatus.APPROVED, BookingStatus.CHECKED_IN, BookingSource.ONLINE);

        assertThat(checkEligible(p)).isFalse();
    }

    @Test
    @DisplayName("✗ ONLINE + APPROVED + CONFIRMED — chưa check-in, không quyết toán")
    void notEligible_confirmed() {
        Payment p = buildPayment(PaymentStatus.APPROVED, BookingStatus.CONFIRMED, BookingSource.ONLINE);

        assertThat(checkEligible(p)).isFalse();
    }

    @Test
    @DisplayName("✗ ONLINE + CANCELLED — không quyết toán")
    void notEligible_cancelled() {
        Payment p = buildPayment(PaymentStatus.CANCELLED, BookingStatus.CANCELLED, BookingSource.ONLINE);

        assertThat(checkEligible(p)).isFalse();
    }
}
