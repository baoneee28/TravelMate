package com.travelmate.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RevenueItemDto — 1 dòng doanh thu tương ứng với 1 payment.
 *
 * Dùng trong bảng danh sách doanh thu ở trang admin và partner.
 *
 * Bổ sung v2: roomCategory, effectiveCommissionRate, commissionSource
 * để phân biệt "Theo loại lưu trú" vs "Theo loại phòng".
 */
@Getter
@Setter
@NoArgsConstructor
public class RevenueItemDto {

    // ─── Booking info ────────────────────────────────────────
    private String bookingCode;
    private String partnerName;
    private String accommodationName;
    private String roomName;
    private String propertyType;      // HOTEL / VILLA / HOMESTAY / RESORT

    // ─── Room category ───────────────────────────────────────
    private String roomCategory;      // STANDARD / DELUXE / FAMILY / VIP / SUITE / OTHER

    // ─── Payment info ────────────────────────────────────────
    private String paymentOption;     // FULL_PAYMENT / DEPOSIT_30
    private String paymentStatus;     // APPROVED / DEPOSIT_FORFEITED

    // ─── Financial breakdown ─────────────────────────────────
    private BigDecimal grossAmount;              // Tiền admin thu online (30% cọc hoặc 100%)
    private BigDecimal commissionBase;           // Cơ sở tính CK (totalAmount cho DEPOSIT_30 COMPLETED, paidAmount còn lại)
    private String     commissionBaseLabel;      // Nhãn hiển thị cơ sở CK
    private BigDecimal commissionRate;           // Tỷ lệ CK hiệu lực (thập phân, VD: 0.15)
    private BigDecimal effectiveCommissionRate;  // Tỷ lệ CK hiệu lực (giống commissionRate, tường minh hơn)
    private BigDecimal commissionAmount;         // = commissionBase × commissionRate
    private BigDecimal voucherDeductionAmount;   // Giảm giá voucher do partner chịu
    private BigDecimal partnerNetAmount;         // = grossAmount - commission - voucherDeduction (từ admin trả partner)

    // ─── Commission source ───────────────────────────────────
    /**
     * Nguồn tính commission:
     *   "ROOM_OVERRIDE"        : phòng có rate riêng
     *   "PROPERTY_TYPE_DEFAULT": dùng mặc định theo loại lưu trú
     */
    private String commissionSource;

    // ─── Voucher info (nếu có) ───────────────────────────────
    private String voucherCode;
    private String voucherCostBearer;    // ADMIN / PARTNER / null

    // ─── Timestamp ───────────────────────────────────────────
    private LocalDateTime createdAt;

    // ─── Helper display ──────────────────────────────────────

    public String getPaymentStatusVN() {
        if ("APPROVED".equals(paymentStatus))           return "Đã thanh toán";
        if ("DEPOSIT_FORFEITED".equals(paymentStatus))  return "Giữ cọc (no-show)";
        return paymentStatus;
    }

    public String getPaymentOptionVN() {
        if ("FULL_PAYMENT".equals(paymentOption)) return "Thanh toán đủ";
        if ("DEPOSIT_30".equals(paymentOption))   return "Cọc 30%";
        return paymentOption;
    }

    public String getPropertyTypeVN() {
        if ("HOTEL".equals(propertyType))    return "Khách sạn";
        if ("VILLA".equals(propertyType))    return "Villa";
        if ("HOMESTAY".equals(propertyType)) return "Homestay";
        if ("RESORT".equals(propertyType))   return "Resort";
        return propertyType;
    }

    /**
     * Mapping roomCategory → Tiếng Việt.
     */
    public String getRoomCategoryVN() {
        if (roomCategory == null) return "Tiêu chuẩn";
        return switch (roomCategory) {
            case "STANDARD" -> "Phòng tiêu chuẩn";
            case "DELUXE"   -> "Phòng Deluxe";
            case "FAMILY"   -> "Phòng gia đình";
            case "VIP"      -> "Phòng VIP";
            case "SUITE"    -> "Phòng Suite";
            case "OTHER"    -> "Khác";
            default         -> roomCategory;
        };
    }

    /**
     * Nguồn commission → Tiếng Việt.
     */
    public String getCommissionSourceVN() {
        if ("ROOM_OVERRIDE".equals(commissionSource)) return "Theo loại phòng";
        return "Theo loại lưu trú";
    }

    /**
     * Hiển thị % commission hiệu lực (VD: "15.0%", "18.0%").
     */
    public String getEffectiveRateDisplay() {
        BigDecimal r = effectiveCommissionRate != null ? effectiveCommissionRate : commissionRate;
        if (r == null) return "—";
        return r.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }
}
