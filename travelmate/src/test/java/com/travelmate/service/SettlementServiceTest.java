package com.travelmate.service;

import com.travelmate.dto.SettlementDetailItemDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PaymentOption;
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
import java.util.Map;
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
        partner.setPartnerPropertyType(PropertyType.HOTEL);

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
    @DisplayName("Settlement đơn cọc hoàn tất chỉ tính hoa hồng trên tiền online dù snapshot cũ từng lưu tổng đơn")
    void generateMonthlySettlement_completedDepositUsesOnlinePaidRule() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        Payment payment = completedPaymentForPartner(lastMonth.atDay(16), new BigDecimal("300000"));
        payment.setPaymentOption(com.travelmate.entity.enums.PaymentOption.DEPOSIT_30);
        Booking booking = payment.getBooking();
        booking.setTotalAmount(new BigDecimal("1000000"));
        booking.setCommissionRateSnapshot(new BigDecimal("0.1500"));
        // Du lieu legacy theo Huong B: service phai bo qua amount cu va ap dung Huong A.
        booking.setCommissionBaseAmount(new BigDecimal("1000000"));
        booking.setCommissionAmountSnapshot(new BigDecimal("150000"));
        booking.setPartnerVoucherAmountSnapshot(BigDecimal.ZERO);
        booking.setPartnerPayoutSnapshot(new BigDecimal("150000"));

        when(paymentRepository.findByPaymentStatusIn(any())).thenReturn(List.of(payment));
        when(userRepository.findAll()).thenReturn(List.of(partner));
        when(settlementRepository.existsByPartnerAndPeriodStartAndPeriodEnd(
                eq(partner), eq(lastMonth.atDay(1)), eq(lastMonth.atEndOfMonth()))).thenReturn(false);

        PartnerSettlement settlement = settlementService.generateMonthlySettlements().get(0);

        assertThat(settlement.getGrossAmount()).isEqualByComparingTo("300000");
        assertThat(settlement.getCommissionAmount()).isEqualByComparingTo("45000");
        assertThat(settlement.getPayoutAmount()).isEqualByComparingTo("255000");
        verify(commissionService, never()).calculateCommission(any(BigDecimal.class), any(Room.class));
    }

    @Test
    @DisplayName("Settlement đơn cọc bị hủy tính hoa hồng trên tiền cọc bị giữ, không ghi tiền tại cơ sở")
    void generateMonthlySettlement_cancelledDepositForfeitedUsesDepositBase() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        Payment payment = completedPaymentForPartner(lastMonth.atDay(17), new BigDecimal("300000"));
        payment.setPaymentOption(com.travelmate.entity.enums.PaymentOption.DEPOSIT_30);
        payment.setPaymentStatus(PaymentStatus.DEPOSIT_FORFEITED);
        Booking booking = payment.getBooking();
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setTotalAmount(new BigDecimal("1000000"));
        booking.setCommissionRateSnapshot(new BigDecimal("0.1500"));
        booking.setCommissionBaseAmount(new BigDecimal("300000"));
        booking.setCommissionAmountSnapshot(new BigDecimal("45000"));
        booking.setPartnerVoucherAmountSnapshot(BigDecimal.ZERO);
        booking.setPartnerPayoutSnapshot(new BigDecimal("255000"));
        booking.setOnsiteAmountSnapshot(BigDecimal.ZERO);

        when(paymentRepository.findByPaymentStatusIn(any())).thenReturn(List.of(payment));
        when(userRepository.findAll()).thenReturn(List.of(partner));
        when(settlementRepository.existsByPartnerAndPeriodStartAndPeriodEnd(
                eq(partner), eq(lastMonth.atDay(1)), eq(lastMonth.atEndOfMonth()))).thenReturn(false);

        PartnerSettlement settlement = settlementService.generateMonthlySettlements().get(0);

        assertThat(settlement.getGrossAmount()).isEqualByComparingTo("300000");
        assertThat(settlement.getCommissionAmount()).isEqualByComparingTo("45000");
        assertThat(settlement.getPayoutAmount()).isEqualByComparingTo("255000");
        verify(commissionService, never()).calculateCommission(any(BigDecimal.class), any(Room.class));
    }

    @Test
    @DisplayName("Settlement đang chờ chi trả tự cập nhật tổng tiền theo Hướng A trước khi hiển thị")
    void pendingSettlement_refreshesLegacyTotalsBeforeDisplay() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        PartnerSettlement pending = pendingSettlement(YearMonth.now().atDay(10));
        pending.setPeriodStart(lastMonth.atDay(1));
        pending.setPeriodEnd(lastMonth.atEndOfMonth());
        pending.setGrossAmount(new BigDecimal("300000"));
        pending.setCommissionAmount(new BigDecimal("150000"));
        pending.setPayoutAmount(new BigDecimal("150000"));
        pending.setVoucherDeductionAmount(BigDecimal.ZERO);

        Payment payment = completedPaymentForPartner(lastMonth.atDay(16), new BigDecimal("300000"));
        payment.setPaymentOption(com.travelmate.entity.enums.PaymentOption.DEPOSIT_30);
        Booking booking = payment.getBooking();
        booking.setBookingCode("BK-ONLINE-DEP-01");
        booking.setTotalAmount(new BigDecimal("1000000"));
        booking.setCommissionRateSnapshot(new BigDecimal("0.1500"));
        booking.setCommissionSourceSnapshot("PROPERTY_TYPE_DEFAULT");
        booking.setPartnerVoucherAmountSnapshot(BigDecimal.ZERO);

        when(settlementRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(pending));
        when(paymentRepository.findByBookingAccommodationOwnerAndPaymentStatusIn(partner, List.of(
                PaymentStatus.APPROVED, PaymentStatus.DEPOSIT_FORFEITED))).thenReturn(List.of(payment));

        List<PartnerSettlement> result = settlementService.getAllSettlementsForAdmin();

        assertThat(result.get(0).getCommissionAmount()).isEqualByComparingTo("45000");
        assertThat(result.get(0).getPayoutAmount()).isEqualByComparingTo("255000");
        verify(settlementRepository).save(pending);
    }

    @Test
    @DisplayName("Đơn cọc 10 triệu có voucher Partner: chỉ tính hoa hồng trên 3 triệu online")
    void depositWithPartnerVoucher_usesOnlineCommissionBaseAndDeductsPartnerVoucher() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        PartnerSettlement settlement = settlementForMonth(lastMonth);
        Payment payment = completedPaymentForPartner(lastMonth.atDay(18), new BigDecimal("3000000"));
        payment.setPaymentOption(PaymentOption.DEPOSIT_30);
        Booking booking = payment.getBooking();
        booking.setBookingCode("BK-DEP-PARTNER-VOUCHER");
        booking.setTotalAmount(new BigDecimal("10000000"));
        booking.setRemainingAmount(new BigDecimal("7000000"));
        booking.setOnsiteAmountSnapshot(new BigDecimal("7000000"));
        booking.setCommissionRateSnapshot(new BigDecimal("0.1500"));
        booking.setCommissionSourceSnapshot("PROPERTY_TYPE_DEFAULT");
        booking.setPartnerVoucherAmountSnapshot(new BigDecimal("200000"));
        booking.setVoucherCostBearer(VoucherCostBearer.PARTNER);
        booking.setDiscountAmount(new BigDecimal("200000"));

        when(paymentRepository.findByBookingAccommodationOwnerAndPaymentStatusIn(partner, List.of(
                PaymentStatus.APPROVED, PaymentStatus.DEPOSIT_FORFEITED))).thenReturn(List.of(payment));

        SettlementDetailItemDto item = settlementService.getBreakdownForSettlement(settlement).get(0);

        assertThat(item.getGross()).isEqualByComparingTo("3000000");
        assertThat(item.getOnsiteAmount()).isEqualByComparingTo("7000000");
        assertThat(item.getCommissionAmount()).isEqualByComparingTo("450000");
        assertThat(item.getVoucherDeductAmount()).isEqualByComparingTo("200000");
        assertThat(item.getPartnerPayout()).isEqualByComparingTo("2350000");
        assertThat(item.getCommissionBaseLabel()).isEqualTo("HH trên cọc online 30%");
    }

    @Test
    @DisplayName("Đơn cọc có voucher Admin: voucher không trừ tiền Partner")
    void depositWithAdminVoucher_doesNotReducePartnerPayout() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        PartnerSettlement settlement = settlementForMonth(lastMonth);
        Payment payment = completedPaymentForPartner(lastMonth.atDay(18), new BigDecimal("3000000"));
        payment.setPaymentOption(PaymentOption.DEPOSIT_30);
        Booking booking = payment.getBooking();
        booking.setBookingCode("BK-DEP-ADMIN-VOUCHER");
        booking.setTotalAmount(new BigDecimal("10000000"));
        booking.setRemainingAmount(new BigDecimal("7000000"));
        booking.setOnsiteAmountSnapshot(new BigDecimal("7000000"));
        booking.setCommissionRateSnapshot(new BigDecimal("0.1500"));
        booking.setCommissionSourceSnapshot("PROPERTY_TYPE_DEFAULT");
        booking.setPartnerVoucherAmountSnapshot(BigDecimal.ZERO);
        booking.setVoucherCostBearer(VoucherCostBearer.ADMIN);
        booking.setDiscountAmount(new BigDecimal("200000"));

        when(paymentRepository.findByBookingAccommodationOwnerAndPaymentStatusIn(partner, List.of(
                PaymentStatus.APPROVED, PaymentStatus.DEPOSIT_FORFEITED))).thenReturn(List.of(payment));

        SettlementDetailItemDto item = settlementService.getBreakdownForSettlement(settlement).get(0);

        assertThat(item.getCommissionAmount()).isEqualByComparingTo("450000");
        assertThat(item.getVoucherDeductAmount()).isEqualByComparingTo("0");
        assertThat(item.getPartnerPayout()).isEqualByComparingTo("2550000");
    }

    @Test
    @DisplayName("Tổng settlement FULL + cọc khớp dòng chi tiết theo Hướng A")
    void mixedFullAndDepositBreakdown_totalsMatchOnlineAmounts() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        PartnerSettlement settlement = settlementForMonth(lastMonth);

        Payment full = completedPaymentForPartner(lastMonth.atDay(19), new BigDecimal("10000000"));
        full.setPaymentOption(PaymentOption.FULL_PAYMENT);
        full.getBooking().setBookingCode("BK-FULL");
        full.getBooking().setTotalAmount(new BigDecimal("10000000"));
        full.getBooking().setCommissionRateSnapshot(new BigDecimal("0.1500"));
        full.getBooking().setPartnerVoucherAmountSnapshot(BigDecimal.ZERO);
        full.getBooking().setOnsiteAmountSnapshot(BigDecimal.ZERO);

        Payment deposit = completedPaymentForPartner(lastMonth.atDay(20), new BigDecimal("3000000"));
        deposit.setPaymentOption(PaymentOption.DEPOSIT_30);
        deposit.getBooking().setBookingCode("BK-DEPOSIT");
        deposit.getBooking().setTotalAmount(new BigDecimal("10000000"));
        deposit.getBooking().setRemainingAmount(new BigDecimal("7000000"));
        deposit.getBooking().setOnsiteAmountSnapshot(new BigDecimal("7000000"));
        deposit.getBooking().setCommissionRateSnapshot(new BigDecimal("0.1500"));
        deposit.getBooking().setPartnerVoucherAmountSnapshot(BigDecimal.ZERO);

        when(paymentRepository.findByBookingAccommodationOwnerAndPaymentStatusIn(partner, List.of(
                PaymentStatus.APPROVED, PaymentStatus.DEPOSIT_FORFEITED))).thenReturn(List.of(full, deposit));

        Map<String, BigDecimal> totals =
                settlementService.calculateBreakdownTotals(settlementService.getBreakdownForSettlement(settlement));

        assertThat(totals.get("gross")).isEqualByComparingTo("13000000");
        assertThat(totals.get("onsite")).isEqualByComparingTo("7000000");
        assertThat(totals.get("commission")).isEqualByComparingTo("1950000");
        assertThat(totals.get("payout")).isEqualByComparingTo("11050000");
    }

    @Test
    @DisplayName("Thanh toán đủ 10 triệu: chỉ voucher Partner bị trừ payout")
    void fullPayment_voucherBearerControlsPartnerDeduction() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        PartnerSettlement settlement = settlementForMonth(lastMonth);

        Payment partnerVoucher = completedPaymentForPartner(lastMonth.atDay(22), new BigDecimal("10000000"));
        partnerVoucher.setPaymentOption(PaymentOption.FULL_PAYMENT);
        partnerVoucher.getBooking().setBookingCode("BK-FULL-PARTNER-VOUCHER");
        partnerVoucher.getBooking().setTotalAmount(new BigDecimal("10000000"));
        partnerVoucher.getBooking().setCommissionRateSnapshot(new BigDecimal("0.1500"));
        partnerVoucher.getBooking().setPartnerVoucherAmountSnapshot(new BigDecimal("200000"));
        partnerVoucher.getBooking().setVoucherCostBearer(VoucherCostBearer.PARTNER);

        Payment adminVoucher = completedPaymentForPartner(lastMonth.atDay(23), new BigDecimal("10000000"));
        adminVoucher.setPaymentOption(PaymentOption.FULL_PAYMENT);
        adminVoucher.getBooking().setBookingCode("BK-FULL-ADMIN-VOUCHER");
        adminVoucher.getBooking().setTotalAmount(new BigDecimal("10000000"));
        adminVoucher.getBooking().setCommissionRateSnapshot(new BigDecimal("0.1500"));
        adminVoucher.getBooking().setPartnerVoucherAmountSnapshot(BigDecimal.ZERO);
        adminVoucher.getBooking().setVoucherCostBearer(VoucherCostBearer.ADMIN);
        adminVoucher.getBooking().setDiscountAmount(new BigDecimal("200000"));

        when(paymentRepository.findByBookingAccommodationOwnerAndPaymentStatusIn(partner, List.of(
                PaymentStatus.APPROVED, PaymentStatus.DEPOSIT_FORFEITED)))
                .thenReturn(List.of(partnerVoucher, adminVoucher));

        List<SettlementDetailItemDto> items = settlementService.getBreakdownForSettlement(settlement);

        assertThat(items.get(0).getCommissionAmount()).isEqualByComparingTo("1500000");
        assertThat(items.get(0).getVoucherDeductAmount()).isEqualByComparingTo("200000");
        assertThat(items.get(0).getPartnerPayout()).isEqualByComparingTo("8300000");
        assertThat(items.get(1).getCommissionAmount()).isEqualByComparingTo("1500000");
        assertThat(items.get(1).getVoucherDeductAmount()).isEqualByComparingTo("0");
        assertThat(items.get(1).getPartnerPayout()).isEqualByComparingTo("8500000");
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
    @DisplayName("Chi trả settlement cọc cũ phải đối soát Hướng A trước khi cộng ví")
    void markPaid_refreshesLegacyDepositTotalsBeforeWalletCredit() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        PartnerSettlement pending = settlementForMonth(lastMonth);
        pending.setGrossAmount(new BigDecimal("3000000"));
        pending.setCommissionAmount(new BigDecimal("1500000"));
        pending.setVoucherDeductionAmount(BigDecimal.ZERO);
        pending.setPayoutAmount(new BigDecimal("1500000"));

        Payment payment = completedPaymentForPartner(lastMonth.atDay(21), new BigDecimal("3000000"));
        payment.setPaymentOption(PaymentOption.DEPOSIT_30);
        Booking booking = payment.getBooking();
        booking.setBookingCode("BK-LEGACY-DEP");
        booking.setTotalAmount(new BigDecimal("10000000"));
        booking.setRemainingAmount(new BigDecimal("7000000"));
        booking.setOnsiteAmountSnapshot(new BigDecimal("7000000"));
        booking.setCommissionRateSnapshot(new BigDecimal("0.1500"));
        booking.setCommissionSourceSnapshot("PROPERTY_TYPE_DEFAULT");
        booking.setPartnerVoucherAmountSnapshot(BigDecimal.ZERO);

        when(settlementRepository.findById(11L)).thenReturn(Optional.of(pending));
        when(paymentRepository.findByBookingAccommodationOwnerAndPaymentStatusIn(partner, List.of(
                PaymentStatus.APPROVED, PaymentStatus.DEPOSIT_FORFEITED))).thenReturn(List.of(payment));

        PartnerSettlement result = settlementService.markSettlementPaid(11L, "Chi trả đã đối soát", partner);

        assertThat(result.getSettlementStatus()).isEqualTo(SettlementStatus.PAID);
        assertThat(result.getCommissionAmount()).isEqualByComparingTo("450000");
        assertThat(result.getPayoutAmount()).isEqualByComparingTo("2550000");
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

    private PartnerSettlement settlementForMonth(YearMonth month) {
        PartnerSettlement settlement = pendingSettlement(LocalDate.now().minusDays(1));
        settlement.setPeriodStart(month.atDay(1));
        settlement.setPeriodEnd(month.atEndOfMonth());
        return settlement;
    }
}
