package com.travelmate.entity;

import com.travelmate.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PartnerSettlement — Bản ghi quyết toán tháng cho partner.
 *
 * Luồng:
 *  1. Admin bấm "Tạo quyết toán tháng trước" → tạo PartnerSettlement PENDING cho tháng trước
 *  2. Admin kiểm tra số liệu, bấm "Đánh dấu đã thanh toán" → PAID
 *
 * Công thức:
 *  payoutAmount = grossAmount - commissionAmount - voucherDeductionAmount
 *
 * grossAmount:             Tổng tiền thu được từ user trong kỳ (Payment APPROVED + DEPOSIT_FORFEITED)
 * commissionAmount:        Chiết khấu nền tảng (theo loại lưu trú)
 * voucherDeductionAmount:  Tổng discountAmount từ booking có voucherCostBearer=PARTNER trong kỳ
 * payoutAmount:            Số tiền admin hoàn trả cho partner
 */
@Entity
@Table(name = "partner_settlements",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_partner_period",
           columnNames = {"partner_id", "period_start", "period_end"}
       ))
@Getter
@Setter
@NoArgsConstructor
public class PartnerSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Partner được quyết toán */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", nullable = false)
    private User partner;

    /** Ngày bắt đầu kỳ quyết toán tháng */
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    /** Ngày kết thúc kỳ quyết toán tháng */
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /** Tổng doanh thu gộp trong kỳ (từ payment APPROVED + DEPOSIT_FORFEITED) */
    @Column(name = "gross_amount", precision = 15, scale = 0)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    /** Tổng chiết khấu nền tảng thu được */
    @Column(name = "commission_amount", precision = 15, scale = 0)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    /** Tổng giảm giá voucher do partner chịu trong kỳ */
    @Column(name = "voucher_deduction_amount", precision = 15, scale = 0)
    private BigDecimal voucherDeductionAmount = BigDecimal.ZERO;

    /** Số tiền admin hoàn trả cho partner = gross - commission - voucherDeduction */
    @Column(name = "payout_amount", precision = 15, scale = 0)
    private BigDecimal payoutAmount = BigDecimal.ZERO;

    /** Trạng thái: PENDING / PAID / CANCELLED */
    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", length = 20)
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    /** Thời điểm admin đánh dấu đã thanh toán */
    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    /** Ghi chú của admin */
    @Column(length = 500)
    private String note;

    /** Thời điểm tạo settlement */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
