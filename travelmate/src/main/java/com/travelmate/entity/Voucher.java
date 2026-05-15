package com.travelmate.entity;

import com.travelmate.entity.enums.DiscountType;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.entity.enums.VoucherScope;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Voucher — Entity ánh xạ bảng `vouchers`.
 *
 * Hai loại chính:
 *  1. Admin tạo (USER_GLOBAL, costBearer=ADMIN):
 *     user dùng khi đặt phòng → admin chịu chi phí giảm giá,
 *     không trừ vào settlement của partner.
 *  2. Partner tạo (PARTNER_ROOM / PARTNER_ACCOMMODATION, costBearer=PARTNER):
 *     user dùng khi đặt phòng của partner đó → trừ vào settlement của partner.
 */
@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã voucher — unique, user nhập khi đặt phòng */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** Tên chương trình ưu đãi */
    @Column(length = 255)
    private String name;

    /** Mô tả chi tiết */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Loại giảm giá: PERCENT hoặc FIXED_AMOUNT */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType = DiscountType.PERCENT;

    /** Giá trị giảm: nếu PERCENT thì đơn vị là %, nếu FIXED_AMOUNT thì đơn vị VNĐ */
    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    /**
     * Giảm tối đa (chỉ dùng với PERCENT).
     * VD: 10% nhưng tối đa 500.000đ
     */
    @Column(name = "max_discount_amount", precision = 15, scale = 0)
    private BigDecimal maxDiscountAmount;

    /** Giá trị đơn tối thiểu để áp dụng voucher */
    @Column(name = "min_order_amount", precision = 15, scale = 0)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    /** Ngày bắt đầu hiệu lực */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** Ngày hết hạn */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** Trạng thái: true = đang hoạt động */
    @Column(nullable = false)
    private Boolean active = true;

    /** Phạm vi voucher: USER_GLOBAL / PARTNER_ACCOMMODATION / PARTNER_ROOM */
    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_scope", nullable = false, length = 30)
    private VoucherScope voucherScope = VoucherScope.USER_GLOBAL;

    /**
     * Ai chịu chi phí giảm giá:
     * - ADMIN  → admin chịu, không trừ settlement partner
     * - PARTNER → trừ vào settlement partner
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cost_bearer", nullable = false, length = 20)
    private VoucherCostBearer costBearer = VoucherCostBearer.ADMIN;

    /**
     * Partner sở hữu voucher (chỉ có khi PARTNER_ROOM / PARTNER_ACCOMMODATION).
     * Admin voucher: null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /**
     * Accommodation gắn với voucher (chỉ có khi PARTNER_ACCOMMODATION).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id")
    private Accommodation accommodation;

    /**
     * Room gắn với voucher (chỉ có khi PARTNER_ROOM).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    /** Thời gian tạo */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ─── Business helpers ──────────────────────────────────────────────────

    /** Kiểm tra voucher còn hiệu lực không (ngày + active) */
    public boolean isCurrentlyValid() {
        if (!Boolean.TRUE.equals(active)) return false;
        LocalDate today = LocalDate.now();
        if (startDate != null && today.isBefore(startDate)) return false;
        if (endDate != null && today.isAfter(endDate)) return false;
        return true;
    }
}
