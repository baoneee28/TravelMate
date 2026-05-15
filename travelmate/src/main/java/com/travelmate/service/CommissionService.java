package com.travelmate.service;

import com.travelmate.entity.Room;
import com.travelmate.entity.enums.PropertyType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * CommissionService — Tính hoa hồng nền tảng.
 *
 * Bảng chiết khấu mặc định theo loại lưu trú:
 *   HOTEL    → 15%
 *   VILLA    → 12%
 *   HOMESTAY → 10%
 *   RESORT   → 18%
 *
 * Nếu Room có commissionRateOverride (không null) thì dùng rate đó.
 * Ngược lại, fallback về default theo PropertyType.
 *
 * ⚠️ DB lưu theo %, VD: 18.00 nghĩa là 18%.
 *    Khi tính: commissionAmount = gross × (rate / 100)
 *    commissionRate trả về dạng thập phân (VD: 0.15, 0.18).
 */
@Service
public class CommissionService {

    // ─── Rate mặc định theo PropertyType ────────────────────────────────────

    /**
     * Trả về tỷ lệ hoa hồng THẬP PHÂN theo loại lưu trú.
     * VD: HOTEL → 0.15
     */
    public BigDecimal getCommissionRate(PropertyType type) {
        if (type == null) return BigDecimal.valueOf(0.10);
        return switch (type) {
            case HOTEL    -> BigDecimal.valueOf(0.15);
            case VILLA    -> BigDecimal.valueOf(0.12);
            case HOMESTAY -> BigDecimal.valueOf(0.10);
            case RESORT   -> BigDecimal.valueOf(0.18);
        };
    }

    /**
     * Trả về % hiển thị (VD: 15, 12, 10, 18).
     */
    public int getCommissionPercent(PropertyType type) {
        return getCommissionRate(type).multiply(BigDecimal.valueOf(100)).intValue();
    }

    // ─── Effective rate (ưu tiên room override) ─────────────────────────────

    /**
     * Trả về tỷ lệ hoa hồng THẬP PHÂN hiệu lực cho 1 phòng cụ thể.
     *
     * Logic:
     *   1. Nếu room != null và room.commissionRateOverride != null
     *      → dùng commissionRateOverride / 100 (vì DB lưu dạng %)
     *   2. Ngược lại → fallback về getCommissionRate(PropertyType)
     *
     * @param room         Room entity (có thể null)
     * @param propertyType Loại lưu trú fallback
     * @return rate thập phân (VD: 0.15, 0.18)
     */
    public BigDecimal getEffectiveCommissionRate(Room room, PropertyType propertyType) {
        if (room != null && room.getCommissionRateOverride() != null) {
            // DB lưu theo % (VD: 18.00), chia 100 để được thập phân
            return room.getCommissionRateOverride()
                       .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }
        return getCommissionRate(propertyType);
    }

    /**
     * Overload tiện dụng: lấy PropertyType từ room.accommodation.
     */
    public BigDecimal getEffectiveCommissionRate(Room room) {
        PropertyType type = (room != null && room.getAccommodation() != null)
                ? room.getAccommodation().getPropertyType()
                : null;
        return getEffectiveCommissionRate(room, type);
    }

    /**
     * Nguồn commission: "Theo loại phòng" hay "Theo loại lưu trú"?
     *
     * @return true nếu dùng room override, false nếu dùng default
     */
    public boolean isRoomOverride(Room room) {
        return room != null && room.getCommissionRateOverride() != null;
    }

    // ─── Tính commission amount ──────────────────────────────────────────────

    /**
     * Tính hoa hồng theo PropertyType (backward compat).
     */
    public BigDecimal calculateCommission(BigDecimal gross, PropertyType type) {
        if (gross == null || gross.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return gross.multiply(getCommissionRate(type)).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính hoa hồng theo Room (ưu tiên override, fallback PropertyType).
     *
     * @param gross Tổng tiền thu được
     * @param room  Room entity
     * @return Số tiền hoa hồng
     */
    public BigDecimal calculateCommission(BigDecimal gross, Room room) {
        if (gross == null || gross.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal rate = getEffectiveCommissionRate(room);
        return gross.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính tiền partner nhận (chưa trừ voucher) — backward compat theo PropertyType.
     */
    public BigDecimal calculatePartnerPayout(BigDecimal gross, PropertyType type) {
        return gross.subtract(calculateCommission(gross, type));
    }

    /**
     * Tính tiền partner nhận (chưa trừ voucher) — v2: ưu tiên room override.
     *
     * @param gross Tổng tiền thu (gross amount)
     * @param room  Room entity (có thể null → fallback PropertyType từ accommodation)
     * @return gross - commission(room)
     */
    public BigDecimal calculatePartnerPayout(BigDecimal gross, Room room) {
        if (gross == null || gross.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return gross.subtract(calculateCommission(gross, room));
    }
}
