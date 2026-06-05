package com.travelmate.service;

import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.PartnerWallet;
import com.travelmate.entity.PartnerWalletTransaction;
import com.travelmate.entity.PartnerWithdrawalRequest;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.PartnerWalletTransactionType;
import com.travelmate.entity.enums.PartnerWithdrawalStatus;
import com.travelmate.entity.enums.SettlementStatus;
import com.travelmate.repository.PartnerSettlementRepository;
import com.travelmate.repository.PartnerWalletRepository;
import com.travelmate.repository.PartnerWalletTransactionRepository;
import com.travelmate.repository.PartnerWithdrawalRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartnerWalletService — Ví quyết toán Partner")
class PartnerWalletServiceTest {

    @Mock private PartnerWalletRepository walletRepository;
    @Mock private PartnerWalletTransactionRepository transactionRepository;
    @Mock private PartnerWithdrawalRequestRepository withdrawalRepository;
    @Mock private PartnerSettlementRepository settlementRepository;

    private PartnerWalletService walletService;
    private User partner;
    private User admin;
    private PartnerWallet wallet;

    @BeforeEach
    void setUp() {
        walletService = new PartnerWalletService(
                walletRepository, transactionRepository, withdrawalRepository, settlementRepository
        );

        partner = new User();
        partner.setId(3L);
        partner.setEmail("partner@travelmate.vn");
        partner.setName("Partner Demo");
        partner.setRole(User.Role.PARTNER);
        partner.setBankName("Vietcombank");
        partner.setBankAccountNumber("0123456789");
        partner.setBankAccountHolder("NGUYEN VAN PARTNER");

        admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@travelmate.vn");
        admin.setRole(User.Role.ADMIN);

        wallet = new PartnerWallet();
        wallet.setId(10L);
        wallet.setPartner(partner);
        wallet.setAvailableBalance(new BigDecimal("3000000"));
        wallet.setPendingWithdrawalAmount(BigDecimal.ZERO);
        wallet.setTotalEarnedAmount(new BigDecimal("3000000"));
        wallet.setTotalWithdrawnAmount(BigDecimal.ZERO);

        lenient().when(walletRepository.save(any(PartnerWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(transactionRepository.save(any(PartnerWalletTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(withdrawalRepository.save(any(PartnerWithdrawalRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PartnerSettlement paidSettlement(String payout) {
        PartnerSettlement settlement = new PartnerSettlement();
        settlement.setId(99L);
        settlement.setPartner(partner);
        settlement.setPeriodStart(LocalDate.of(2026, 4, 1));
        settlement.setPeriodEnd(LocalDate.of(2026, 4, 30));
        settlement.setPayoutAmount(new BigDecimal(payout));
        settlement.setSettlementStatus(SettlementStatus.PAID);
        return settlement;
    }

    @Test
    @DisplayName("PAID settlement cộng payout vào ví và ghi transaction")
    void creditSettlement_addsBalanceAndTransaction() {
        PartnerSettlement settlement = paidSettlement("1200000");
        when(walletRepository.findByPartner(partner)).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsBySettlementAndTransactionType(
                settlement, PartnerWalletTransactionType.SETTLEMENT_CREDIT)).thenReturn(false);

        PartnerWallet result = walletService.creditSettlement(settlement, admin);

        assertThat(result.getAvailableBalance()).isEqualByComparingTo("4200000");
        assertThat(result.getTotalEarnedAmount()).isEqualByComparingTo("4200000");

        ArgumentCaptor<PartnerWalletTransaction> txCaptor = ArgumentCaptor.forClass(PartnerWalletTransaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(PartnerWalletTransactionType.SETTLEMENT_CREDIT);
        assertThat(txCaptor.getValue().getBalanceBefore()).isEqualByComparingTo("3000000");
        assertThat(txCaptor.getValue().getBalanceAfter()).isEqualByComparingTo("4200000");
    }

    @Test
    @DisplayName("Ví đơn cọc chỉ cộng payout online 2.550.000, không cộng 70% tại cơ sở")
    void creditSettlement_depositCreditsOnlyOnlinePayout() {
        PartnerSettlement settlement = paidSettlement("2550000");
        when(walletRepository.findByPartner(partner)).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsBySettlementAndTransactionType(
                settlement, PartnerWalletTransactionType.SETTLEMENT_CREDIT)).thenReturn(false);

        PartnerWallet result = walletService.creditSettlement(settlement, admin);

        assertThat(result.getAvailableBalance()).isEqualByComparingTo("5550000");
        assertThat(result.getAvailableBalance()).isNotEqualByComparingTo("12550000");

        ArgumentCaptor<PartnerWalletTransaction> txCaptor = ArgumentCaptor.forClass(PartnerWalletTransaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getAmount()).isEqualByComparingTo("2550000");
    }

    @Test
    @DisplayName("Không cộng ví nếu settlement chưa PAID")
    void creditSettlement_shouldRejectPendingSettlement() {
        PartnerSettlement settlement = paidSettlement("1200000");
        settlement.setSettlementStatus(SettlementStatus.PENDING);

        assertThatThrownBy(() -> walletService.creditSettlement(settlement, admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chỉ settlement đã thanh toán");

        verify(walletRepository, never()).save(any(PartnerWallet.class));
        verify(transactionRepository, never()).save(any(PartnerWalletTransaction.class));
    }

    @Test
    @DisplayName("Settlement đã credit rồi thì không cộng tiền lần hai")
    void creditSettlement_preventsDoubleCredit() {
        PartnerSettlement settlement = paidSettlement("1200000");
        when(walletRepository.findByPartner(partner)).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsBySettlementAndTransactionType(
                settlement, PartnerWalletTransactionType.SETTLEMENT_CREDIT)).thenReturn(true);

        PartnerWallet result = walletService.creditSettlement(settlement, admin);

        assertThat(result.getAvailableBalance()).isEqualByComparingTo("3000000");
        verify(transactionRepository, never()).save(any(PartnerWalletTransaction.class));
    }

    @Test
    @DisplayName("Partner thiếu bank info thì không được rút tiền")
    void requestWithdrawal_requiresBankInfo() {
        partner.setBankAccountNumber(null);

        assertThatThrownBy(() -> walletService.requestWithdrawal(partner, new BigDecimal("500000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cập nhật đầy đủ");
    }

    @Test
    @DisplayName("Yêu cầu rút hợp lệ: available giảm, pending tăng, ghi transaction OUT")
    void requestWithdrawal_updatesWallet() {
        when(walletRepository.findByPartnerForUpdate(partner)).thenReturn(Optional.of(wallet));

        PartnerWithdrawalRequest request = walletService.requestWithdrawal(partner, new BigDecimal("1000000"));

        assertThat(request.getWithdrawalStatus()).isEqualTo(PartnerWithdrawalStatus.PENDING);
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("2000000");
        assertThat(wallet.getPendingWithdrawalAmount()).isEqualByComparingTo("1000000");

        ArgumentCaptor<PartnerWalletTransaction> txCaptor = ArgumentCaptor.forClass(PartnerWalletTransaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getTransactionType()).isEqualTo(PartnerWalletTransactionType.WITHDRAWAL_REQUEST);
        assertThat(txCaptor.getValue().getBalanceAfter()).isEqualByComparingTo("2000000");
    }

    @Test
    @DisplayName("Không cho rút khi số tiền lớn hơn số dư khả dụng")
    void requestWithdrawal_shouldFailWhenAmountGreaterThanBalance() {
        when(walletRepository.findByPartnerForUpdate(partner)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.requestWithdrawal(partner, new BigDecimal("3500000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Số dư ví không đủ");

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("3000000");
        assertThat(wallet.getPendingWithdrawalAmount()).isEqualByComparingTo("0");
        verify(withdrawalRepository, never()).save(any(PartnerWithdrawalRequest.class));
        verify(transactionRepository, never()).save(any(PartnerWalletTransaction.class));
    }

    @Test
    @DisplayName("Admin xác nhận đã chuyển khoản: pending giảm, totalWithdrawn tăng")
    void markWithdrawalPaid_updatesPendingAndTotalWithdrawn() {
        PartnerWithdrawalRequest request = new PartnerWithdrawalRequest();
        request.setId(5L);
        request.setPartner(partner);
        request.setRequestCode("WD-DEMO");
        request.setAmount(new BigDecimal("1000000"));
        request.setWithdrawalStatus(PartnerWithdrawalStatus.PENDING);

        wallet.setAvailableBalance(new BigDecimal("2000000"));
        wallet.setPendingWithdrawalAmount(new BigDecimal("1000000"));

        when(withdrawalRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(request));
        when(walletRepository.findByPartnerForUpdate(partner)).thenReturn(Optional.of(wallet));

        PartnerWithdrawalRequest result = walletService.markWithdrawalPaid(5L, admin, "Admin ghi nhận đã xử lý ngoài hệ thống");

        assertThat(result.getWithdrawalStatus()).isEqualTo(PartnerWithdrawalStatus.PAID);
        assertThat(wallet.getPendingWithdrawalAmount()).isEqualByComparingTo("0");
        assertThat(wallet.getTotalWithdrawnAmount()).isEqualByComparingTo("1000000");
    }

    @Test
    @DisplayName("Admin không được xác nhận lại yêu cầu đã xử lý")
    void markWithdrawalPaid_shouldFailWhenAlreadyProcessed() {
        PartnerWithdrawalRequest request = new PartnerWithdrawalRequest();
        request.setId(7L);
        request.setPartner(partner);
        request.setRequestCode("WD-PAID");
        request.setAmount(new BigDecimal("1000000"));
        request.setWithdrawalStatus(PartnerWithdrawalStatus.PAID);

        when(withdrawalRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> walletService.markWithdrawalPaid(7L, admin, "Chuyển lại"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã được xử lý");

        verify(walletRepository, never()).save(any(PartnerWallet.class));
        verify(transactionRepository, never()).save(any(PartnerWalletTransaction.class));
    }

    @Test
    @DisplayName("Admin từ chối: hoàn tiền vào available và giảm pending")
    void rejectWithdrawal_refundsAvailableBalance() {
        PartnerWithdrawalRequest request = new PartnerWithdrawalRequest();
        request.setId(6L);
        request.setPartner(partner);
        request.setRequestCode("WD-REJECT");
        request.setAmount(new BigDecimal("1000000"));
        request.setWithdrawalStatus(PartnerWithdrawalStatus.PENDING);

        wallet.setAvailableBalance(new BigDecimal("2000000"));
        wallet.setPendingWithdrawalAmount(new BigDecimal("1000000"));

        when(withdrawalRepository.findByIdForUpdate(6L)).thenReturn(Optional.of(request));
        when(walletRepository.findByPartnerForUpdate(partner)).thenReturn(Optional.of(wallet));

        PartnerWithdrawalRequest result = walletService.rejectWithdrawal(6L, admin, "Sai STK");

        assertThat(result.getWithdrawalStatus()).isEqualTo(PartnerWithdrawalStatus.REJECTED);
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("3000000");
        assertThat(wallet.getPendingWithdrawalAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Admin không được từ chối lại yêu cầu đã xử lý")
    void rejectWithdrawal_shouldFailWhenAlreadyProcessed() {
        PartnerWithdrawalRequest request = new PartnerWithdrawalRequest();
        request.setId(8L);
        request.setPartner(partner);
        request.setRequestCode("WD-REJECTED");
        request.setAmount(new BigDecimal("1000000"));
        request.setWithdrawalStatus(PartnerWithdrawalStatus.REJECTED);

        when(withdrawalRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> walletService.rejectWithdrawal(8L, admin, "Từ chối lại"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã được xử lý");

        verify(walletRepository, never()).save(any(PartnerWallet.class));
        verify(transactionRepository, never()).save(any(PartnerWalletTransaction.class));
    }
}
