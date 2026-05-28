package com.travelmate.service;

import com.travelmate.dto.AdminRevenueSummaryDto;
import com.travelmate.dto.PartnerRevenueSummaryDto;
import com.travelmate.dto.RevenueItemDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevenueService - Doanh thu Partner Homestay")
class RevenueServiceHomestayTest {

    @Mock private PaymentRepository paymentRepository;

    private RevenueService revenueService;
    private User homestayPartner;
    private Accommodation homestay;
    private Room room;

    @BeforeEach
    void setUp() {
        revenueService = new RevenueService(paymentRepository, new CommissionService());
        homestayPartner = new User();
        homestayPartner.setId(8L);
        homestayPartner.setName("Mekong Homestay");
        homestayPartner.setRole(User.Role.PARTNER);
        homestayPartner.setPartnerPropertyType(PropertyType.HOMESTAY);

        homestay = new Accommodation();
        homestay.setId(6L);
        homestay.setName("Hoa Lư Riverside Homestay");
        homestay.setOwner(homestayPartner);
        homestay.setPropertyType(PropertyType.HOMESTAY);

        room = new Room();
        room.setRoomName("Riverside Deluxe");
        room.setAccommodation(homestay);
    }

    @Test
    @DisplayName("Homestay mặc định tính commission 10% và trừ voucher Partner chịu")
    void partnerSummary_usesHomestayRateAndPartnerVoucherDeduction() {
        Payment online = payment("960000", BookingSource.ONLINE);
        online.getBooking().setVoucherCostBearer(VoucherCostBearer.PARTNER);
        online.getBooking().setDiscountAmount(new BigDecimal("50000"));

        when(paymentRepository.findByBookingAccommodationOwnerAndPaymentStatusIn(
                org.mockito.ArgumentMatchers.eq(homestayPartner), any()))
                .thenReturn(List.of(online));

        PartnerRevenueSummaryDto summary = revenueService.calculatePartnerRevenueSummary(homestayPartner);

        assertThat(summary.getTotalGross()).isEqualByComparingTo("960000");
        assertThat(summary.getTotalCommission()).isEqualByComparingTo("96000");
        assertThat(summary.getTotalVoucherDeduction()).isEqualByComparingTo("50000");
        assertThat(summary.getTotalPayout()).isEqualByComparingTo("814000");
    }

    @Test
    @DisplayName("Revenue Homestay loại booking trực tiếp dù dữ liệu cũ có payment APPROVED")
    void partnerSummary_excludesDirectPaymentFromRevenue() {
        Payment online = payment("960000", BookingSource.ONLINE);
        Payment direct = payment("780000", BookingSource.DIRECT);

        when(paymentRepository.findByBookingAccommodationOwnerAndPaymentStatusIn(
                org.mockito.ArgumentMatchers.eq(homestayPartner), any()))
                .thenReturn(List.of(online, direct));

        PartnerRevenueSummaryDto summary = revenueService.calculatePartnerRevenueSummary(homestayPartner);

        assertThat(summary.getTotalBookings()).isEqualTo(1);
        assertThat(summary.getTotalGross()).isEqualByComparingTo("960000");
        assertThat(summary.getTotalCommission()).isEqualByComparingTo("96000");
    }

    @Test
    @DisplayName("Dashboard Admin cũng không cộng payment trực tiếp vào doanh thu TravelMate")
    void adminSummary_excludesDirectPaymentFromRevenue() {
        Payment online = payment("960000", BookingSource.ONLINE);
        Payment direct = payment("780000", BookingSource.DIRECT);

        when(paymentRepository.findByPaymentStatusIn(any())).thenReturn(List.of(online, direct));

        AdminRevenueSummaryDto summary = revenueService.calculateAdminRevenueSummary();

        assertThat(summary.getTotalGross()).isEqualByComparingTo("960000");
        assertThat(summary.getTotalCommission()).isEqualByComparingTo("96000");
    }

    @Test
    @DisplayName("Đơn cọc hoàn tất giữ rate snapshot nhưng chỉ tính hoa hồng trên tiền online")
    void completedDeposit_usesFrozenRateAndOnlinePaidCommissionBase() {
        Payment deposit = payment("288000", BookingSource.ONLINE);
        deposit.setPaymentOption(PaymentOption.DEPOSIT_30);
        deposit.getBooking().setPaymentOption(PaymentOption.DEPOSIT_30);
        deposit.getBooking().setTotalAmount(new BigDecimal("960000"));
        deposit.getBooking().setRemainingAmount(new BigDecimal("672000"));
        deposit.getBooking().setCommissionRateSnapshot(new BigDecimal("0.1000"));
        deposit.getBooking().setCommissionSourceSnapshot("PROPERTY_TYPE_DEFAULT");
        // Gia lap snapshot cu tung tinh tren tong don; man hinh phai tu chuan hoa theo Huong A.
        deposit.getBooking().setCommissionBaseAmount(new BigDecimal("960000"));
        deposit.getBooking().setCommissionAmountSnapshot(new BigDecimal("96000"));
        deposit.getBooking().setOnlinePaidAmountSnapshot(new BigDecimal("288000"));
        deposit.getBooking().setOnsiteAmountSnapshot(new BigDecimal("672000"));
        deposit.getBooking().setPartnerVoucherAmountSnapshot(BigDecimal.ZERO);
        deposit.getBooking().setPartnerPayoutSnapshot(new BigDecimal("192000"));
        room.setCommissionRateOverride(new BigDecimal("20.00"));

        when(paymentRepository.findByPaymentStatusIn(any())).thenReturn(List.of(deposit));

        RevenueItemDto item = revenueService.getRevenueItemsForAdmin().get(0);

        assertThat(item.getGrossAmount()).isEqualByComparingTo("288000");
        assertThat(item.getOnsiteAmount()).isEqualByComparingTo("672000");
        assertThat(item.getCommissionBase()).isEqualByComparingTo("288000");
        assertThat(item.getCommissionAmount()).isEqualByComparingTo("28800");
        assertThat(item.getEffectiveCommissionRate()).isEqualByComparingTo("0.1000");
        assertThat(item.getPartnerNetAmount()).isEqualByComparingTo("259200");
    }

    private Payment payment(String amount, BookingSource source) {
        Booking booking = new Booking();
        booking.setAccommodation(homestay);
        booking.setRoom(room);
        booking.setBookingSource(source);
        booking.setPaymentOption(PaymentOption.FULL_PAYMENT);
        booking.setDiscountAmount(BigDecimal.ZERO);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentOption(PaymentOption.FULL_PAYMENT);
        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setAmount(new BigDecimal(amount));
        return payment;
    }
}
