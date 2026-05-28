package com.travelmate.service;

import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.PartnerWallet;
import com.travelmate.entity.PartnerWalletTransaction;
import com.travelmate.entity.PartnerWithdrawalRequest;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.PartnerWalletTransactionType;
import com.travelmate.entity.enums.PartnerWithdrawalStatus;
import com.travelmate.entity.enums.SettlementStatus;
import com.travelmate.entity.enums.WalletTransactionDirection;
import com.travelmate.repository.PartnerWalletRepository;
import com.travelmate.repository.PartnerWalletTransactionRepository;
import com.travelmate.repository.PartnerWithdrawalRequestRepository;
import com.travelmate.repository.PartnerSettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PartnerWalletService {

    private static final String BANK_ACCOUNT_PATTERN = "\\d{6,30}";

    private final PartnerWalletRepository walletRepository;
    private final PartnerWalletTransactionRepository transactionRepository;
    private final PartnerWithdrawalRequestRepository withdrawalRepository;
    private final PartnerSettlementRepository settlementRepository;

    public PartnerWalletService(PartnerWalletRepository walletRepository,
                                PartnerWalletTransactionRepository transactionRepository,
                                PartnerWithdrawalRequestRepository withdrawalRepository,
                                PartnerSettlementRepository settlementRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public PartnerWallet getOrCreateWallet(User partner) {
        if (partner == null || partner.getId() == null) {
            throw new IllegalArgumentException("Partner không hợp lệ!");
        }

        return walletRepository.findByPartner(partner)
                .orElseGet(() -> {
                    PartnerWallet wallet = new PartnerWallet();
                    wallet.setPartner(partner);
                    wallet.setAvailableBalance(BigDecimal.ZERO);
                    wallet.setPendingWithdrawalAmount(BigDecimal.ZERO);
                    wallet.setTotalEarnedAmount(BigDecimal.ZERO);
                    wallet.setTotalWithdrawnAmount(BigDecimal.ZERO);
                    return walletRepository.save(wallet);
                });
    }

    @Transactional
    public PartnerWallet ensureWalletWithPaidSettlements(User partner) {
        PartnerWallet wallet = getOrCreateWallet(partner);
        // Đồng bộ dữ liệu ban đầu: settlement PAID có sẵn trong SQL sẽ được backfill vào ví.
        // Luồng chính vẫn là Admin mark settlement PAID -> creditSettlement().
        List<PartnerSettlement> paidSettlements = settlementRepository
                .findByPartnerAndSettlementStatusOrderBySettlementDateAsc(partner, SettlementStatus.PAID);
        for (PartnerSettlement settlement : paidSettlements) {
            if (!transactionRepository.existsBySettlementAndTransactionType(
                    settlement, PartnerWalletTransactionType.SETTLEMENT_CREDIT)) {
                wallet = creditSettlement(settlement, null);
            }
        }
        return wallet;
    }

    @Transactional
    public PartnerWallet creditSettlement(PartnerSettlement settlement, User admin) {
        if (settlement == null || settlement.getPartner() == null) {
            throw new IllegalArgumentException("Settlement không hợp lệ!");
        }
        if (settlement.getSettlementStatus() != SettlementStatus.PAID) {
            throw new IllegalArgumentException("Chỉ settlement đã thanh toán mới được cộng vào ví!");
        }

        boolean credited = transactionRepository.existsBySettlementAndTransactionType(
                settlement, PartnerWalletTransactionType.SETTLEMENT_CREDIT);
        if (credited) {
            return getOrCreateWallet(settlement.getPartner());
        }

        PartnerWallet wallet = getOrCreateWallet(settlement.getPartner());
        BigDecimal amount = zeroIfNull(settlement.getPayoutAmount());
        BigDecimal before = zeroIfNull(wallet.getAvailableBalance());
        BigDecimal after = before.add(amount);

        wallet.setAvailableBalance(after);
        wallet.setTotalEarnedAmount(zeroIfNull(wallet.getTotalEarnedAmount()).add(amount));
        PartnerWallet savedWallet = walletRepository.save(wallet);

        createTransaction(
                settlement.getPartner(),
                settlement,
                null,
                PartnerWalletTransactionType.SETTLEMENT_CREDIT,
                WalletTransactionDirection.IN,
                amount,
                before,
                after,
                "Cộng tiền quyết toán kỳ " + formatSettlementPeriod(settlement),
                admin
        );

        return savedWallet;
    }

    @Transactional
    public PartnerWithdrawalRequest requestWithdrawal(User partner, BigDecimal amount) {
        validateBankInfo(partner);
        BigDecimal requestedAmount = validatePositiveAmount(amount);
        PartnerWallet wallet = walletRepository.findByPartnerForUpdate(partner)
                .orElseGet(() -> getOrCreateWallet(partner));

        BigDecimal available = zeroIfNull(wallet.getAvailableBalance());
        if (requestedAmount.compareTo(available) > 0) {
            throw new IllegalArgumentException("Số dư ví không đủ để rút!");
        }

        BigDecimal before = available;
        BigDecimal after = before.subtract(requestedAmount);

        wallet.setAvailableBalance(after);
        wallet.setPendingWithdrawalAmount(zeroIfNull(wallet.getPendingWithdrawalAmount()).add(requestedAmount));

        PartnerWithdrawalRequest request = new PartnerWithdrawalRequest();
        request.setPartner(partner);
        request.setRequestCode(generateWithdrawalCode());
        request.setAmount(requestedAmount);
        request.setBankName(partner.getBankName().trim());
        request.setBankAccountNumber(partner.getBankAccountNumber().trim());
        request.setBankAccountHolder(partner.getBankAccountHolder().trim());
        request.setBankBranch(trimToNull(partner.getBankBranch()));
        request.setWithdrawalStatus(PartnerWithdrawalStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());

        PartnerWithdrawalRequest savedRequest = withdrawalRepository.save(request);
        walletRepository.save(wallet);

        createTransaction(
                partner,
                null,
                savedRequest,
                PartnerWalletTransactionType.WITHDRAWAL_REQUEST,
                WalletTransactionDirection.OUT,
                requestedAmount,
                before,
                after,
                "Partner gửi yêu cầu rút tiền về tài khoản " + savedRequest.getMaskedBankAccountNumber(),
                null
        );

        return savedRequest;
    }

    @Transactional
    public PartnerWithdrawalRequest markWithdrawalPaid(Long withdrawalId, User admin, String adminNote) {
        PartnerWithdrawalRequest request = findWithdrawalForUpdate(withdrawalId);
        ensurePending(request);
        ensureNoWithdrawalTransaction(request, PartnerWalletTransactionType.WITHDRAWAL_PAID);

        PartnerWallet wallet = walletRepository.findByPartnerForUpdate(request.getPartner())
                .orElseGet(() -> getOrCreateWallet(request.getPartner()));
        BigDecimal amount = zeroIfNull(request.getAmount());

        wallet.setPendingWithdrawalAmount(subtractFloorZero(wallet.getPendingWithdrawalAmount(), amount));
        wallet.setTotalWithdrawnAmount(zeroIfNull(wallet.getTotalWithdrawnAmount()).add(amount));

        request.setWithdrawalStatus(PartnerWithdrawalStatus.PAID);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdmin(admin);
        request.setAdminNote(trimToNull(adminNote));

        walletRepository.save(wallet);
        PartnerWithdrawalRequest savedRequest = withdrawalRepository.save(request);

        BigDecimal currentBalance = zeroIfNull(wallet.getAvailableBalance());
        createTransaction(
                request.getPartner(),
                null,
                request,
                PartnerWalletTransactionType.WITHDRAWAL_PAID,
                WalletTransactionDirection.INFO,
                amount,
                currentBalance,
                currentBalance,
                "Admin xác nhận đã chuyển khoản ngoài hệ thống",
                admin
        );

        return savedRequest;
    }

    @Transactional
    public PartnerWithdrawalRequest rejectWithdrawal(Long withdrawalId, User admin, String adminNote) {
        PartnerWithdrawalRequest request = findWithdrawalForUpdate(withdrawalId);
        ensurePending(request);
        ensureNoWithdrawalTransaction(request, PartnerWalletTransactionType.WITHDRAWAL_REJECTED);

        PartnerWallet wallet = walletRepository.findByPartnerForUpdate(request.getPartner())
                .orElseGet(() -> getOrCreateWallet(request.getPartner()));
        BigDecimal amount = zeroIfNull(request.getAmount());
        BigDecimal before = zeroIfNull(wallet.getAvailableBalance());
        BigDecimal after = before.add(amount);

        wallet.setAvailableBalance(after);
        wallet.setPendingWithdrawalAmount(subtractFloorZero(wallet.getPendingWithdrawalAmount(), amount));

        request.setWithdrawalStatus(PartnerWithdrawalStatus.REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdmin(admin);
        request.setAdminNote(trimToNull(adminNote));

        walletRepository.save(wallet);
        PartnerWithdrawalRequest savedRequest = withdrawalRepository.save(request);

        createTransaction(
                request.getPartner(),
                null,
                request,
                PartnerWalletTransactionType.WITHDRAWAL_REJECTED,
                WalletTransactionDirection.IN,
                amount,
                before,
                after,
                "Hoàn lại số dư vì yêu cầu rút tiền bị từ chối",
                admin
        );

        return savedRequest;
    }

    public List<PartnerWalletTransaction> getTransactions(User partner) {
        return transactionRepository.findByPartnerOrderByCreatedAtDesc(partner);
    }

    public List<PartnerWithdrawalRequest> getWithdrawalsForPartner(User partner) {
        return withdrawalRepository.findByPartnerOrderByRequestedAtDesc(partner);
    }

    public List<PartnerWithdrawalRequest> getAllWithdrawalsForAdmin() {
        return withdrawalRepository.findAllByOrderByRequestedAtDesc();
    }

    public long countPendingWithdrawals() {
        return withdrawalRepository.countByWithdrawalStatus(PartnerWithdrawalStatus.PENDING);
    }

    private PartnerWithdrawalRequest findWithdrawalForUpdate(Long withdrawalId) {
        return withdrawalRepository.findByIdForUpdate(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Yêu cầu rút tiền không tồn tại!"));
    }

    private void ensurePending(PartnerWithdrawalRequest request) {
        if (request.getWithdrawalStatus() != PartnerWithdrawalStatus.PENDING) {
            throw new IllegalArgumentException("Yêu cầu rút tiền đã được xử lý!");
        }
    }

    private void ensureNoWithdrawalTransaction(PartnerWithdrawalRequest request,
                                               PartnerWalletTransactionType transactionType) {
        if (transactionRepository.existsByWithdrawalRequestAndTransactionType(request, transactionType)) {
            throw new IllegalArgumentException("Yêu cầu rút tiền đã có lịch sử xử lý tương ứng!");
        }
    }

    private void validateBankInfo(User partner) {
        if (partner == null || partner.getId() == null) {
            throw new IllegalArgumentException("Partner không hợp lệ!");
        }
        if (!StringUtils.hasText(partner.getBankName())
                || !StringUtils.hasText(partner.getBankAccountNumber())
                || !StringUtils.hasText(partner.getBankAccountHolder())) {
            throw new IllegalArgumentException("Bạn cần cập nhật đầy đủ ngân hàng, số tài khoản và chủ tài khoản trước khi rút tiền!");
        }
        if (!partner.getBankAccountNumber().trim().matches(BANK_ACCOUNT_PATTERN)) {
            throw new IllegalArgumentException("Số tài khoản chỉ được gồm chữ số, từ 6 đến 30 ký tự!");
        }
    }

    private BigDecimal validatePositiveAmount(BigDecimal amount) {
        BigDecimal safeAmount = zeroIfNull(amount);
        if (safeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0!");
        }
        return safeAmount;
    }

    private PartnerWalletTransaction createTransaction(User partner,
                                                       PartnerSettlement settlement,
                                                       PartnerWithdrawalRequest withdrawalRequest,
                                                       PartnerWalletTransactionType type,
                                                       WalletTransactionDirection direction,
                                                       BigDecimal amount,
                                                       BigDecimal balanceBefore,
                                                       BigDecimal balanceAfter,
                                                       String description,
                                                       User admin) {
        PartnerWalletTransaction transaction = new PartnerWalletTransaction();
        transaction.setPartner(partner);
        transaction.setSettlement(settlement);
        transaction.setWithdrawalRequest(withdrawalRequest);
        transaction.setTransactionCode(generateTransactionCode());
        transaction.setTransactionType(type);
        transaction.setDirection(direction);
        transaction.setAmount(zeroIfNull(amount));
        transaction.setBalanceBefore(zeroIfNull(balanceBefore));
        transaction.setBalanceAfter(zeroIfNull(balanceAfter));
        transaction.setDescription(description);
        transaction.setCreatedByAdmin(admin);
        return transactionRepository.save(transaction);
    }

    private String generateWithdrawalCode() {
        String code;
        do {
            code = "WD" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (withdrawalRepository.existsByRequestCode(code));
        return code;
    }

    private String generateTransactionCode() {
        String code;
        do {
            code = "WTX" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
                    + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (transactionRepository.existsByTransactionCode(code));
        return code;
    }

    private String formatSettlementPeriod(PartnerSettlement settlement) {
        if (settlement.getPeriodStart() == null || settlement.getPeriodEnd() == null) {
            return "#" + settlement.getId();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return settlement.getPeriodStart().format(formatter) + " - " + settlement.getPeriodEnd().format(formatter);
    }

    private BigDecimal subtractFloorZero(BigDecimal current, BigDecimal amount) {
        BigDecimal result = zeroIfNull(current).subtract(zeroIfNull(amount));
        return result.max(BigDecimal.ZERO);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
