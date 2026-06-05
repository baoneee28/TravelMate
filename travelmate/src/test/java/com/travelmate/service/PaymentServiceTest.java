package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PartnerBookingStatus;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private NotificationService notificationService;
    @Mock private EmailService emailService;

    @InjectMocks private PaymentService paymentService;

    @Test
    void markGatewaySuccessAutoConfirmsPaymentAndHoldsRoom() {
        User user = new User();
        Accommodation accommodation = new Accommodation();
        accommodation.setName("LATA Hotel");
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking.setBookingCode("BK-LATA-000001");
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentOption(PaymentOption.FULL_PAYMENT);
        payment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        when(paymentRepository.findByVnpTxnRef("TM-001")).thenReturn(Optional.of(payment));

        boolean updated = paymentService.markGatewaySuccess("TM-001", Map.of(
                "vnp_TransactionNo", "123456",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00"));

        assertThat(updated).isTrue();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getApprovedAt()).isNotNull();
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(booking.getPartnerStatus()).isEqualTo(PartnerBookingStatus.PARTNER_CONFIRMED);
        verify(paymentRepository).save(payment);
        verify(bookingRepository).save(booking);
        verify(notificationService).createPaymentReceived(user, "BK-LATA-000001", accommodation, "thanh toán 100%");
        verify(emailService).sendBookingConfirmationEmail(booking);
    }

    @Test
    void markGatewaySuccessDeposit30AutoHoldsRoomAndKeepsRemainingAmountForCheckIn() {
        User user = new User();
        Accommodation accommodation = new Accommodation();
        accommodation.setName("Hoa Lu Riverside Homestay");
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking.setBookingCode("BK-HLR-STD-0007");
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        booking.setTotalAmount(new BigDecimal("960000"));
        booking.setPaidAmount(new BigDecimal("288000"));
        booking.setRemainingAmount(new BigDecimal("672000"));

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentOption(PaymentOption.DEPOSIT_30);
        payment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        when(paymentRepository.findByVnpTxnRef("TM-DEPOSIT")).thenReturn(Optional.of(payment));

        boolean updated = paymentService.markGatewaySuccess("TM-DEPOSIT", Map.of(
                "vnp_TransactionNo", "789012",
                "vnp_ResponseCode", "00",
                "vnp_TransactionStatus", "00"));

        assertThat(updated).isTrue();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(booking.getPartnerStatus()).isEqualTo(PartnerBookingStatus.PARTNER_CONFIRMED);
        assertThat(booking.getPaidAmount()).isEqualByComparingTo("288000");
        assertThat(booking.getRemainingAmount()).isEqualByComparingTo("672000");
        verify(paymentRepository).save(payment);
        verify(bookingRepository).save(booking);
        verify(notificationService).createPaymentReceived(user, "BK-HLR-STD-0007", accommodation, "khoản cọc 30%");
        verify(emailService).sendBookingConfirmationEmail(booking);
    }

    @Test
    void markGatewaySuccessIsIdempotentWhenUserRefreshesPaymentResult() {
        Booking booking = new Booking();
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentOption(PaymentOption.FULL_PAYMENT);
        payment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        when(paymentRepository.findByVnpTxnRef("TM-F5")).thenReturn(Optional.of(payment));

        assertThat(paymentService.markGatewaySuccess("TM-F5", Map.of("vnp_ResponseCode", "00"))).isTrue();
        assertThat(paymentService.markGatewaySuccess("TM-F5", Map.of("vnp_ResponseCode", "00"))).isTrue();

        verify(paymentRepository, times(1)).save(payment);
        verify(bookingRepository, times(1)).save(booking);
        verify(notificationService, times(1)).createPaymentReceived(any(), any(), any(), eq("thanh toán 100%"));
        verify(emailService, times(1)).sendBookingConfirmationEmail(any());
    }

    @Test
    void markGatewayFailedCancelsBookingAndRestoresRoomQuota() {
        Room room = new Room();
        room.setAvailableQuantity(1);

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setRoomQuantity(2);
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        when(paymentRepository.findByVnpTxnRef("TM-CANCEL")).thenReturn(Optional.of(payment));

        paymentService.markGatewayFailed("TM-CANCEL", "24", Map.of("vnp_ResponseCode", "24"));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(room.getAvailableQuantity()).isEqualTo(3);
        verify(paymentRepository).save(payment);
        verify(bookingRepository).save(booking);
        verify(roomRepository).save(room);
        verifyNoInteractions(notificationService);
        verifyNoInteractions(emailService);
    }

    @Test
    void markGatewayFailedExpiredCancelsBookingAndRestoresRoomQuota() {
        Room room = new Room();
        room.setAvailableQuantity(4);

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setRoomQuantity(1);
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        when(paymentRepository.findByVnpTxnRef("TM-EXPIRED")).thenReturn(Optional.of(payment));

        paymentService.markGatewayFailed("TM-EXPIRED", "11", Map.of("vnp_ResponseCode", "11"));

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getPaymentStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(room.getAvailableQuantity()).isEqualTo(5);
        verify(paymentRepository).save(payment);
        verify(bookingRepository).save(booking);
        verify(roomRepository).save(room);
        verifyNoInteractions(emailService);
    }

    @Test
    void markGatewayFailedIsIdempotentAfterAlreadyCancelled() {
        Booking booking = new Booking();
        booking.setRoom(new Room());
        booking.setRoomQuantity(1);
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentStatus(PaymentStatus.CANCELLED);
        when(paymentRepository.findByVnpTxnRef("TM-CANCELLED")).thenReturn(Optional.of(payment));

        paymentService.markGatewayFailed("TM-CANCELLED", "24", Map.of("vnp_ResponseCode", "24"));

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(roomRepository, never()).save(any(Room.class));
        verifyNoInteractions(emailService);
    }
}
