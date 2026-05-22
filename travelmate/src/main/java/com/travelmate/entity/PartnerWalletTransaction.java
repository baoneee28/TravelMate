package com.travelmate.entity;

import com.travelmate.entity.enums.PartnerWalletTransactionType;
import com.travelmate.entity.enums.WalletTransactionDirection;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
public class PartnerWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private User partner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id")
    private PartnerSettlement settlement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_request_id")
    private PartnerWithdrawalRequest withdrawalRequest;

    @Column(name = "transaction_code", nullable = false, unique = true, length = 50)
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 40)
    private PartnerWalletTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private WalletTransactionDirection direction;

    @Column(name = "amount", precision = 15, scale = 0)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "balance_before", precision = 15, scale = 0)
    private BigDecimal balanceBefore = BigDecimal.ZERO;

    @Column(name = "balance_after", precision = 15, scale = 0)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_admin_id")
    private User createdByAdmin;
}
