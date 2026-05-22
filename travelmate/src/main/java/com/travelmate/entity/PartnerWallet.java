package com.travelmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_wallets")
@Getter
@Setter
@NoArgsConstructor
public class PartnerWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false, unique = true)
    private User partner;

    @Column(name = "available_balance", precision = 15, scale = 0)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "pending_withdrawal_amount", precision = 15, scale = 0)
    private BigDecimal pendingWithdrawalAmount = BigDecimal.ZERO;

    @Column(name = "total_earned_amount", precision = 15, scale = 0)
    private BigDecimal totalEarnedAmount = BigDecimal.ZERO;

    @Column(name = "total_withdrawn_amount", precision = 15, scale = 0)
    private BigDecimal totalWithdrawnAmount = BigDecimal.ZERO;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
