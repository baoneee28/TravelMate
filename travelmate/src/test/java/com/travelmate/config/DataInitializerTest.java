package com.travelmate.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DataInitializer - Chuan hoa snapshot tai chinh cu")
class DataInitializerTest {

    @Test
    @DisplayName("Voucher cũ bị tắt và ticket hiển thị được chuẩn hóa theo kho Admin cấp")
    void deactivateLegacyPartnerIssuedVouchers_disablesOldPartnerCreatedRecords() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString())).thenReturn(2);

        ReflectionTestUtils.invokeMethod(
                new DataInitializer(), "deactivateLegacyPartnerIssuedVouchers", jdbcTemplate);
        ReflectionTestUtils.invokeMethod(
                new DataInitializer(), "normalizeVisibleSeedLabels", jdbcTemplate);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture());

        assertThat(String.join("\n", sql.getAllValues())).contains(
                "UPDATE vouchers SET active = 0",
                "owner_id IS NOT NULL",
                "voucher_scope IN ('PARTNER_ROOM', 'PARTNER_ACCOMMODATION')",
                "UPDATE support_tickets SET subject = 'Muốn gắn voucher nhưng không thấy phòng/căn của tôi'",
                "Voucher theo cơ sở");
    }

    @Test
    @DisplayName("Note cu ve xac nhan giu phong duoc doi sang TravelMate da giu")
    void normalizeLegacyBookingHoldNotes_rewritesOldPartnerHoldNotes() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        ReflectionTestUtils.invokeMethod(
                new DataInitializer(), "normalizeLegacyBookingHoldNotes", jdbcTemplate);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture(), args.capture());

        String allSql = String.join("\n", sql.getAllValues());
        assertThat(allSql).contains(
                "UPDATE bookings SET note = ?",
                "UPDATE payments SET note = ?",
                "Xác nhận giữ phòng",
                "Xác nh%n gi% ph%",
                "Partner%đã giữ phòng");
        assertThat(args.getAllValues().stream().flatMap(Arrays::stream).toList()).contains(
                "VNPAY đã ghi nhận thanh toán. TravelMate đã giữ phòng/căn trên hệ thống.",
                "VNPAY ghi nhận thành công. TravelMate đã giữ phòng/căn trên hệ thống.");
    }

    @Test
    @DisplayName("Don coc da luu tru khoi phuc tien tai co so tu tong don tru tien online")
    void normalizeDepositSnapshots_repairsLegacyOnsiteAmount() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString())).thenReturn(1);

        ReflectionTestUtils.invokeMethod(
                new DataInitializer(), "normalizeOnlineDepositCommissionSnapshots", jdbcTemplate);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sql.capture());

        assertThat(sql.getValue()).contains(
                "b.commission_base_amount = " +
                        "COALESCE(b.total_before_discount, b.total_amount, COALESCE(b.paid_amount, 0))",
                "b.partner_payout_snapshot = COALESCE(b.paid_amount, 0) - ",
                "b.remaining_amount = CASE WHEN",
                "GREATEST(COALESCE(b.total_before_discount, b.total_amount, COALESCE(b.paid_amount, 0)) - COALESCE(b.paid_amount, 0), 0)",
                "b.onsite_amount_snapshot = CASE WHEN",
                "b.payment_status = 'DEPOSIT_FORFEITED'",
                "b.booking_status IN ('CANCELLED', 'NO_SHOW')")
                .doesNotContain("b.partner_payout_snapshot = GREATEST");
    }

    @Test
    @DisplayName("Don thanh toan du tinh hoa hong tren tong don goc truoc voucher")
    void normalizeFullPaymentSnapshots_usesPreDiscountCommissionBase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString())).thenReturn(1);

        ReflectionTestUtils.invokeMethod(
                new DataInitializer(), "normalizeOnlineFullPaymentCommissionSnapshots", jdbcTemplate);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sql.capture());

        assertThat(sql.getValue()).contains(
                "WHERE b.payment_option = 'FULL_PAYMENT'",
                "b.commission_base_amount = COALESCE(b.total_before_discount, b.total_amount, COALESCE(b.paid_amount, 0))",
                "b.partner_payout_snapshot = COALESCE(b.paid_amount, 0) - ",
                "b.voucher_cost_bearer = 'PARTNER'",
                "b.remaining_amount = 0",
                "b.online_paid_amount_snapshot = COALESCE(b.paid_amount, 0)",
                "b.onsite_amount_snapshot = 0");
    }

    @Test
    @DisplayName("Review cu tu 5 sao duoc doi mot lan sang thang 10 diem")
    void normalizeLegacyReviewRatings_convertsOldFiveStarRowsOnce() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyString())).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(2);
        when(jdbcTemplate.update(anyString())).thenReturn(2);

        ReflectionTestUtils.invokeMethod(
                new DataInitializer(), "normalizeLegacyReviewRatings", jdbcTemplate);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, atLeastOnce()).update(sql.capture());

        assertThat(String.join("\n", sql.getAllValues())).contains(
                "UPDATE reviews SET rating = LEAST(rating * 2, 10) WHERE rating BETWEEN 1 AND 5",
                "UPDATE accommodations a",
                "ROUND(AVG(rating), 1)",
                "WHERE is_hidden = 0");
        verify(jdbcTemplate).update(
                anyString(),
                eq("review-rating-stars-to-score-v1"));
    }
}
