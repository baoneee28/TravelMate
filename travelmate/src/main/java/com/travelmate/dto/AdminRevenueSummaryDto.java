package com.travelmate.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * AdminRevenueSummaryDto — Tổng hợp doanh thu cho Admin dashboard.
 *
 * Hiển thị ở /admin/revenue: 4 KPI card chính.
 */
@Getter
@Setter
@NoArgsConstructor
public class AdminRevenueSummaryDto {

    /** Tổng tiền admin thu online từ user (APPROVED + DEPOSIT_FORFEITED) */
    private BigDecimal totalGross = BigDecimal.ZERO;

    /** Tổng cơ sở tính commission (= tiền online thực thu: cọ 30% hoặc 100% đã thu qua TravelMate) */
    private BigDecimal totalCommissionBase = BigDecimal.ZERO;

    /** Tổng chiết khấu admin giữ lại */
    private BigDecimal totalCommission = BigDecimal.ZERO;

    /** Tổng số tiền admin sẽ hoàn trả cho các partner (từ phần admin đã thu online) */
    private BigDecimal totalPayout = BigDecimal.ZERO;

    /** Số booking đã duyệt (payment APPROVED) */
    private long totalApproved = 0;

    /** Số booking no-show (payment DEPOSIT_FORFEITED) */
    private long totalForfeited = 0;

    /** Tổng số booking có doanh thu */
    public long getTotalBookings() {
        return totalApproved + totalForfeited;
    }

    /** Tỷ lệ hoa hồng trung bình tính trên cơ sở đúng (commissionBase) */
    public BigDecimal getAvgCommissionRate() {
        if (totalCommissionBase.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return totalCommission.multiply(BigDecimal.valueOf(100))
                .divide(totalCommissionBase, 1, java.math.RoundingMode.HALF_UP);
    }
}
