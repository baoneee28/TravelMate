package com.travelmate.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * SettlementDetailItemDto — 1 dòng booking trong bảng chi tiết settlement.
 *
 * Mỗi payment trong kỳ quyết toán tương ứng 1 dòng với đầy đủ:
 * gross, commission (rate + nguồn), voucher, payout.
 */
@Getter
@Setter
@NoArgsConstructor
public class SettlementDetailItemDto {

    private String bookingCode;
    private String accName;
    private String roomName;

    private String checkIn;
    private String checkOut;

    private BigDecimal totalOrderAmount;
    private BigDecimal gross;
    private BigDecimal onsiteAmount;
    private String commissionBaseLabel;

    /** Ví dụ: "15% (mặc định)" hoặc "10% (theo phòng)" */
    private String commissionRateDisplay;
    private BigDecimal commissionAmount;

    private String voucherCode;
    private BigDecimal voucherDeductAmount;

    private BigDecimal partnerPayout;

    /** Tiếng Việt: "Đã thanh toán" / "Giữ cọc (hủy/no-show)" */
    private String paymentStatusVN;
}
