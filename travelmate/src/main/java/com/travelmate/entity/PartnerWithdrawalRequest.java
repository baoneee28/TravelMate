package com.travelmate.entity;

import com.travelmate.entity.enums.PartnerWithdrawalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "partner_withdrawal_requests")
@Getter
@Setter
@NoArgsConstructor
public class PartnerWithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private User partner;

    @Column(name = "request_code", nullable = false, unique = true, length = 50)
    private String requestCode;

    @Column(name = "amount", precision = 15, scale = 0)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_account_holder", length = 100)
    private String bankAccountHolder;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    @Enumerated(EnumType.STRING)
    @Column(name = "withdrawal_status", length = 20)
    private PartnerWithdrawalStatus withdrawalStatus = PartnerWithdrawalStatus.PENDING;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_admin_id")
    private User processedByAdmin;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    public String getMaskedBankAccountNumber() {
        if (bankAccountNumber == null || bankAccountNumber.isBlank()) {
            return "—";
        }
        String cleaned = bankAccountNumber.trim();
        if (cleaned.length() <= 4) {
            return cleaned;
        }
        return "****" + cleaned.substring(cleaned.length() - 4);
    }
}
