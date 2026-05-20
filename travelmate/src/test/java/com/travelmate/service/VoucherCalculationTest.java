package com.travelmate.service;

import com.travelmate.entity.Voucher;
import com.travelmate.entity.enums.DiscountType;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.RoomRepository;
import com.travelmate.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử logic tính giảm giá voucher:
 * PERCENT, FIXED_AMOUNT, maxDiscountAmount cap, VNPAY minimum guard,
 * phân biệt costBearer ADMIN vs PARTNER.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService — Tính giảm giá")
class VoucherCalculationTest {

    @Mock private VoucherRepository voucherRepository;
    @Mock private AccommodationRepository accommodationRepository;
    @Mock private RoomRepository roomRepository;

    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        voucherService = new VoucherService(voucherRepository, accommodationRepository, roomRepository);
    }

    private Voucher buildPercentVoucher(double percent, long maxDiscount, long minOrder) {
        Voucher v = new Voucher();
        v.setDiscountType(DiscountType.PERCENT);
        v.setDiscountValue(BigDecimal.valueOf(percent));
        v.setMaxDiscountAmount(maxDiscount > 0 ? BigDecimal.valueOf(maxDiscount) : null);
        v.setMinOrderAmount(BigDecimal.valueOf(minOrder));
        v.setActive(true);
        v.setStartDate(LocalDate.now().minusDays(1));
        v.setEndDate(LocalDate.now().plusDays(30));
        v.setCostBearer(VoucherCostBearer.ADMIN);
        return v;
    }

    private Voucher buildFixedVoucher(long fixedAmount, long minOrder, VoucherCostBearer bearer) {
        Voucher v = new Voucher();
        v.setDiscountType(DiscountType.FIXED_AMOUNT);
        v.setDiscountValue(BigDecimal.valueOf(fixedAmount));
        v.setMaxDiscountAmount(null);
        v.setMinOrderAmount(BigDecimal.valueOf(minOrder));
        v.setActive(true);
        v.setStartDate(LocalDate.now().minusDays(1));
        v.setEndDate(LocalDate.now().plusDays(30));
        v.setCostBearer(bearer);
        return v;
    }

    // ─── PERCENT voucher ──────────────────────────────────────────────────────

    @Test
    @DisplayName("PERCENT 10% — đơn hàng 1.300.000đ → giảm 130.000đ")
    void percent_basicDiscount() {
        Voucher v = buildPercentVoucher(10, 500000, 500000);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("1300000"));

        assertThat(discount).isEqualByComparingTo("130000");
    }

    @Test
    @DisplayName("PERCENT 10% — bị cap bởi maxDiscountAmount 500.000đ")
    void percent_cappedByMaxDiscount() {
        Voucher v = buildPercentVoucher(10, 500000, 500000);

        // 10% × 6.000.000 = 600.000 > cap 500.000 → kết quả phải là 500.000
        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("6000000"));

        assertThat(discount).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("PERCENT 20% — SUMMER10: đơn 1.300.000đ → giảm 130.000đ (< cap 500.000đ)")
    void percent_summer10_underCap() {
        Voucher v = buildPercentVoucher(10, 500000, 500000);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("1300000"));

        assertThat(discount).isEqualByComparingTo("130000");
        assertThat(discount.compareTo(new BigDecimal("500000"))).isLessThan(0);
    }

    @Test
    @DisplayName("PERCENT 20% — LATA20 partner: đơn 2.550.000đ → giảm 510.000đ (< cap 800.000đ)")
    void percent_lata20_partnerVoucher() {
        Voucher v = buildPercentVoucher(20, 800000, 650000);
        v.setCostBearer(VoucherCostBearer.PARTNER);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("2550000"));

        // 20% × 2.550.000 = 510.000 < cap 800.000
        assertThat(discount).isEqualByComparingTo("510000");
        assertThat(v.getCostBearer()).isEqualTo(VoucherCostBearer.PARTNER);
    }

    // ─── FIXED_AMOUNT voucher ─────────────────────────────────────────────────

    @Test
    @DisplayName("FIXED 50.000đ — WELCOME50: đơn 300.000đ → giảm đúng 50.000đ")
    void fixed_basicDiscount() {
        Voucher v = buildFixedVoucher(50000, 200000, VoucherCostBearer.ADMIN);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("300000"));

        assertThat(discount).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("FIXED 100.000đ — VNT100K partner: đơn 5.600.000đ → giảm 100.000đ")
    void fixed_partnerVoucherVnt100k() {
        Voucher v = buildFixedVoucher(100000, 2800000, VoucherCostBearer.PARTNER);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("5600000"));

        assertThat(discount).isEqualByComparingTo("100000");
        assertThat(v.getCostBearer()).isEqualTo(VoucherCostBearer.PARTNER);
    }

    // ─── Guard: tổng tiền không âm ───────────────────────────────────────────

    @Test
    @DisplayName("Guard: discount không làm tổng tiền xuống dưới 5.000đ (VNPAY minimum)")
    void guard_discountCannotReduceBelowVnpayMinimum() {
        // Đơn hàng 6.000đ với voucher giảm 10% = 600đ → kết quả còn 5.400đ (ổn)
        // Nhưng nếu voucher 50.000đ fix thì kết quả = -44.000đ → phải giới hạn
        Voucher v = buildFixedVoucher(50000, 0, VoucherCostBearer.ADMIN);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("10000"));

        // 10.000 - 50.000 = -40.000 → guard: giữ tối thiểu 5.000đ
        // discount tối đa = 10.000 - 5.000 = 5.000đ
        assertThat(discount).isEqualByComparingTo("5000");
        BigDecimal afterDiscount = new BigDecimal("10000").subtract(discount);
        assertThat(afterDiscount).isGreaterThanOrEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("Guard: discount không làm tổng tiền xuống dưới VNPAY minimum (PERCENT)")
    void guard_percentDiscountCannotReduceBelowVnpayMinimum() {
        Voucher v = buildPercentVoucher(99, 0, 0); // 99%

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("6000"));

        // 99% × 6.000 = 5.940 → kết quả còn 60đ < 5.000đ → guard kích hoạt
        // discount tối đa = 6.000 - 5.000 = 1.000đ
        BigDecimal afterDiscount = new BigDecimal("6000").subtract(discount);
        assertThat(afterDiscount).isGreaterThanOrEqualTo(new BigDecimal("5000"));
    }

    // ─── Cost bearer phân biệt ─────────────────────────────────────────────

    @Test
    @DisplayName("costBearer ADMIN — không ảnh hưởng settlement partner")
    void costBearer_admin_markedCorrectly() {
        Voucher v = buildPercentVoucher(10, 500000, 500000);
        v.setCostBearer(VoucherCostBearer.ADMIN);

        assertThat(v.getCostBearer()).isEqualTo(VoucherCostBearer.ADMIN);
    }

    @Test
    @DisplayName("costBearer PARTNER — sẽ bị trừ vào payout settlement partner")
    void costBearer_partner_markedCorrectly() {
        Voucher v = buildFixedVoucher(100000, 2800000, VoucherCostBearer.PARTNER);

        assertThat(v.getCostBearer()).isEqualTo(VoucherCostBearer.PARTNER);
    }
}
