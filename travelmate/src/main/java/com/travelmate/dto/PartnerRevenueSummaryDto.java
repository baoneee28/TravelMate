package com.travelmate.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * PartnerRevenueSummaryDto — Tổng hợp doanh thu cho Partner.
 *
 * Hiển thị ở /partner/revenue: tên partner + 4 số liệu chính.
 */
@Getter
@Setter
@NoArgsConstructor
public class PartnerRevenueSummaryDto {

    /** Tên đối tác */
    private String partnerName;

    /** Loại lưu trú (VD: HOTEL) */
    private String propertyType;

    /** Tỷ lệ chiết khấu (VD: 15%) */
    private BigDecimal commissionRate = BigDecimal.ZERO;

    /** Tổng doanh thu gộp (số tiền user đã trả) */
    private BigDecimal totalGross = BigDecimal.ZERO;

    /** Tổng chiết khấu nền tảng trừ đi */
    private BigDecimal totalCommission = BigDecimal.ZERO;

    /** Tổng giảm giá voucher do partner chịu */
    private BigDecimal totalVoucherDeduction = BigDecimal.ZERO;

    /** Tổng tiền partner sẽ nhận = gross - commission - voucherDeduction */
    private BigDecimal totalPayout = BigDecimal.ZERO;

    /** Số booking có doanh thu */
    private long totalBookings = 0;
}
