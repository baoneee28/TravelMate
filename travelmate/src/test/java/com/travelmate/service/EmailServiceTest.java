package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.enums.PaymentOption;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EmailServiceTest {

    @Test
    void bookingConfirmationEmailSaysRoomIsHeldByTravelMateSystem() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> mailSenderProvider = mock(ObjectProvider.class);
        EmailService emailService = new EmailService(
                mailSenderProvider,
                "smtp.example.test",
                "demo@travelmate.vn",
                "secret",
                "demo@travelmate.vn");

        Booking booking = booking("BK-LATA-STD-000094");

        String plainText = ReflectionTestUtils.invokeMethod(
                emailService,
                "buildBookingPlainText",
                booking);
        String html = ReflectionTestUtils.invokeMethod(
                emailService,
                "buildBookingHtmlBody",
                booking);

        assertThat(plainText)
                .contains("Phòng/căn của bạn đã được TravelMate giữ trên hệ thống")
                .doesNotContain("chờ đối tác giữ")
                .doesNotContain("chờ đối tác giữ chỗ");
        assertThat(html)
                .contains("Phòng/căn của bạn đã được TravelMate giữ trên hệ thống")
                .doesNotContain("chờ đối tác giữ")
                .doesNotContain("chờ đối tác giữ chỗ");
    }

    private Booking booking(String bookingCode) {
        Accommodation accommodation = new Accommodation();
        accommodation.setName("LATA Hotel & Apartments");

        Booking booking = new Booking();
        booking.setBookingCode(bookingCode);
        booking.setAccommodation(accommodation);
        booking.setPaymentOption(PaymentOption.FULL_PAYMENT);
        booking.setPaidAmount(BigDecimal.valueOf(450_000));
        booking.setRemainingAmount(BigDecimal.ZERO);
        return booking;
    }
}
