package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.SettlementStatus;
import com.travelmate.entity.enums.VoucherCostBearer;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementService — Quyết toán tháng")
class SettlementServiceTest {

    @Mock private PartnerSettlementRepository settlementRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommissionService commissionService;
    @Mock private PartnerWalletService partnerWalletService;

    private SettlementService settlementService;
    private User partner;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
                settlementRepository, paymentRepository, userRepository, commissionService, partnerWalletService
        );

        partner = new User();
        partner.setId(3L);
        partner.setName("Partner Demo");
        partner.setRole(User.Role.PARTNER);

        lenient().when(settlementRepository.save(any(PartnerSettlement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Tạo settlement tháng trước có ngày chi trả dự kiến mùng 10 tháng hiện tại")
    void generateMonthlySettlement_shouldSetScheduledPayoutDateToDay10() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        Payment payment = completedPaymentForPartner(lastMonth.atDay(15), new BigDecimal("1000000"));

        when(paymentRepository.findByPaymentStatusIn(any())).thenReturn(List.of(payment));
        when(userRepository.findAll()).thenReturn(List.of(partner));
        when(settlementRepository.existsByPartnerAndPeriodStartAndPeriodEnd(
                eq(partner), eq(lastMonth.atDay(1)), eq(lastMonth.atEndOfMonth()))).thenReturn(false);
        when(commissionService.calculateCommission(new BigDecimal("1000000"), payment.getBooking().getRoom()))
                .thenReturn(new BigDecimal("150000"));

        List<PartnerSettlement> created = settlementService.generateMonthlySettlements();

        assertThat(created).hasSize(1);
        PartnerSettlement settlement = created.get(0);
        assertThat(settlement.getPeriodStart()).isEqualTo(lastMonth.atDay(1));
        assertThat(settlement.getPeriodEnd()).isEqualTo(lastMonth.atEndOfMonth());
        assertThat(settlement.getScheduledPayoutDate()).isEqualTo(YearMonth.now().atDay(10));
        assertThat(settlement.getPayoutAmount()).isEqualByComparingTo("850000");
        assertThat(settlement.getSettlementStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("Đánh dấu PAID sẽ lưu trạng thái và cộng payout vào ví Partner")
    void markPaid_shouldCreditPartnerWallet() {
        PartnerSettlement settlement = pendingSettlement(LocalDate.now().minusDays(1));
        when(settlementRepository.findById(11L)).thenReturn(Optional.of(settlement));

        PartnerSettlement result = settlementService.markSettlementPaid(11L, "Đã chuyển khoản", partner);

        assertThat(result.getSettlementStatus()).isEqualTo(SettlementStatus.PAID);
        assertThat(result.getSettlementDate()).isNotNull();
        assertThat(result.getNote()).isEqualTo("Đã chuyển khoản");
        verify(partnerWalletService).creditSettlement(result, partner);
    }

    @Test
    @DisplayName("Demo mode tắt: chưa tới ngày chi trả thì không cho PAID")
    void markPaid_shouldBlockBeforeScheduledPayoutDateWhenDemoModeFalse() {
        PartnerSettlement settlement = pendingSettlement(LocalDate.now().plusDays(2));
        when(settlementRepository.findById(12L)).thenReturn(Optional.of(settlement));
        ReflectionTestUtils.setField(settlementService, "demoMode", false);

        assertThatThrownBy(() -> settlementService.markSettlementPaid(12L, "Trả sớm", partner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chưa đến ngày chi trả dự kiến");

        verify(settlementRepository, never()).save(any(PartnerSettlement.class));
        verify(partnerWalletService, never()).creditSettlement(any(PartnerSettlement.class), any(User.class));
    }

    @Test
    @DisplayName("Demo mode bật: cho phép PAID trước ngày chi trả để trình bày đồ án")
    void markPaid_shouldAllowBeforeScheduledPayoutDateWhenDemoModeTrue() {
        PartnerSettlement settlement = pendingSettlement(LocalDate.now().plusDays(2));
        when(settlementRepository.findById(13L)).thenReturn(Optional.of(settlement));
        ReflectionTestUtils.setField(settlementService, "demoMode", true);

        PartnerSettlement result = settlementService.markSettlementPaid(13L, "Demo chi trả sớm", partner);

        assertThat(result.getSettlementStatus()).isEqualTo(SettlementStatus.PAID);
        verify(partnerWalletService).creditSettlement(result, partner);
    }

    private Payment completedPaymentForPartner(LocalDate approvedDate, BigDecimal amount) {
        Accommodation accommodation = new Accommodation();
        accommodation.setOwner(partner);
        accommodation.setPropertyType(PropertyType.HOTEL);

        Room room = new Room();
        room.setAccommodation(accommodation);

        Booking booking = new Booking();
        booking.setAccommodation(accommodation);
        booking.setRoom(room);
        booking.setBookingStatus(BookingStatus.COMPLETED);
        booking.setBookingSource(BookingSource.ONLINE);
        booking.setVoucherCostBearer(VoucherCostBearer.ADMIN);
        booking.setDiscountAmount(BigDecimal.ZERO);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setPaymentStatus(PaymentStatus.APPROVED);
        payment.setApprovedAt(approvedDate.atStartOfDay());
        return payment;
    }

    private PartnerSettlement pendingSettlement(LocalDate scheduledPayoutDate) {
        PartnerSettlement settlement = new PartnerSettlement();
        settlement.setId(11L);
        settlement.setPartner(partner);
        settlement.setPeriodStart(LocalDate.of(2026, 4, 1));
        settlement.setPeriodEnd(LocalDate.of(2026, 4, 30));
        settlement.setScheduledPayoutDate(scheduledPayoutDate);
        settlement.setPayoutAmount(new BigDecimal("850000"));
        settlement.setSettlementStatus(SettlementStatus.PENDING);
        return settlement;
    }
}
