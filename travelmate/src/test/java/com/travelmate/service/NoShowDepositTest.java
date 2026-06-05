package com.travelmate.service;

import com.travelmate.config.VnpayConfigProperties;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử nghiệp vụ No-Show:
 *
 * DEPOSIT_30 no-show:
 *   → BookingStatus = NO_SHOW
 *   → PaymentStatus = DEPOSIT_FORFEITED (cọc 30% bị giữ)
 *   → Room availableQuantity được mở lại (+= roomQuantity)
 *
 * FULL_PAYMENT no-show:
 *   → Không xử lý tự động trong demo
 *   → Booking/Payment giữ nguyên để Admin xử lý refund/giữ phí theo chính sách riêng
 *   → Không chuyển CHECKED_IN giả
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService — Nghiệp vụ No-Show")
class NoShowDepositTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private AccommodationRepository accommodationRepository;
    @Mock private VoucherService voucherService;
    @Mock private NotificationService notificationService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository, paymentRepository, roomRepository,
                accommodationRepository, voucherService, notificationService,
                new CommissionService(),
                new VnpayConfigProperties()
        );
    }

    private Booking buildConfirmedBooking(Long id, PaymentOption option, int roomQty) {
        Room room = new Room();
        room.setId(10L);
        room.setAvailableQuantity(3);

        Booking booking = new Booking();
        booking.setId(id);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentOption(option);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setRoom(room);
        booking.setRoomQuantity(roomQty);
        booking.setTotalAmount(new BigDecimal("960000"));
        booking.setPaidAmount(new BigDecimal("288000"));
        return booking;
    }

    // ─── DEPOSIT_30 no-show ───────────────────────────────────────────────────

    @Test
    @DisplayName("DEPOSIT_30 no-show → BookingStatus = NO_SHOW")
    void deposit30NoShow_bookingStatusIsNoShow() {
        Booking booking = buildConfirmedBooking(5L, PaymentOption.DEPOSIT_30, 1);
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.APPROVED);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv ->
                java.util.Objects.requireNonNull(inv.<Booking>getArgument(0)));

        Booking result = bookingService.markNoShow(5L);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.NO_SHOW);
    }

    @Test
    @DisplayName("DEPOSIT_30 no-show → PaymentStatus = DEPOSIT_FORFEITED (cọc bị giữ)")
    void deposit30NoShow_paymentStatusIsDepositForfeited() {
        Booking booking = buildConfirmedBooking(5L, PaymentOption.DEPOSIT_30, 1);
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.APPROVED);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv ->
                java.util.Objects.requireNonNull(inv.<Booking>getArgument(0)));

        bookingService.markNoShow(5L);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.DEPOSIT_FORFEITED);
    }

    @Test
    @DisplayName("DEPOSIT_30 no-show → Room được mở lại (+= roomQuantity)")
    void deposit30NoShow_roomAvailabilityRestored() {
        Booking booking = buildConfirmedBooking(5L, PaymentOption.DEPOSIT_30, 2);
        Room room = booking.getRoom();
        int originalQty = room.getAvailableQuantity(); // 3

        Payment payment = new Payment();
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv ->
                java.util.Objects.requireNonNull(inv.<Booking>getArgument(0)));

        bookingService.markNoShow(5L);

        // roomQuantity = 2 → availableQuantity phải tăng lên 3 + 2 = 5
        assertThat(room.getAvailableQuantity()).isEqualTo(originalQty + 2);
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("DEPOSIT_30 no-show → ghi note phù hợp")
    void deposit30NoShow_noteIsSet() {
        Booking booking = buildConfirmedBooking(5L, PaymentOption.DEPOSIT_30, 1);
        Payment payment = new Payment();
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv ->
                java.util.Objects.requireNonNull(inv.<Booking>getArgument(0)));

        Booking result = bookingService.markNoShow(5L);

        assertThat(result.getNote()).contains("Cọc 30% bị giữ lại");
    }

    // ─── FULL_PAYMENT no-show ─────────────────────────────────────────────────

    @Test
    @DisplayName("FULL_PAYMENT no-show tự động bị chặn, booking giữ CONFIRMED")
    void fullPaymentNoShow_isRejectedAndBookingStatusStaysConfirmed() {
        Booking booking = buildConfirmedBooking(6L, PaymentOption.FULL_PAYMENT, 1);

        when(bookingRepository.findById(6L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.markNoShow(6L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chỉ áp dụng cho đơn cọc 30%");

        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("FULL_PAYMENT no-show tự động bị chặn, payment giữ APPROVED")
    void fullPaymentNoShow_isRejectedAndPaymentStatusStaysApproved() {
        Booking booking = buildConfirmedBooking(6L, PaymentOption.FULL_PAYMENT, 1);

        when(bookingRepository.findById(6L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.markNoShow(6L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chính sách hoàn tiền/giữ phí riêng");

        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("FULL_PAYMENT no-show tự động bị chặn, không lưu booking/payment/room")
    void fullPaymentNoShow_isRejectedWithoutSavingSideEffects() {
        Booking booking = buildConfirmedBooking(6L, PaymentOption.FULL_PAYMENT, 1);
        Room room = booking.getRoom();
        int originalQty = room.getAvailableQuantity();

        when(bookingRepository.findById(6L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.markNoShow(6L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không chuyển thành check-in giả");

        assertThat(room.getAvailableQuantity()).isEqualTo(originalQty);
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(paymentRepository, never()).findByBooking(any(Booking.class));
        verify(roomRepository, never()).save(room);
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("No-show booking không tồn tại → ném RuntimeException")
    void noShow_bookingNotFound_throwsException() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.markNoShow(999L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("No-show booking chưa CONFIRMED → ném RuntimeException")
    void noShow_bookingNotConfirmed_throwsException() {
        Booking booking = buildConfirmedBooking(7L, PaymentOption.DEPOSIT_30, 1);
        booking.setBookingStatus(BookingStatus.PENDING_ADMIN_APPROVAL); // chưa duyệt

        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.markNoShow(7L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CONFIRMED");
    }
}
