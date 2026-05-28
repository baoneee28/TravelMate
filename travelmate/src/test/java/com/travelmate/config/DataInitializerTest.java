package com.travelmate.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DataInitializer - Chuan hoa snapshot don coc cu")
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
    @DisplayName("Don coc da luu tru khoi phuc tien tai co so tu tong don tru tien online")
    void normalizeDepositSnapshots_repairsLegacyOnsiteAmount() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.update(anyString())).thenReturn(1);

        ReflectionTestUtils.invokeMethod(
                new DataInitializer(), "normalizeOnlineDepositCommissionSnapshots", jdbcTemplate);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sql.capture());

        assertThat(sql.getValue()).contains(
                "b.commission_base_amount = COALESCE(b.paid_amount, 0)",
                "b.remaining_amount = CASE WHEN",
                "GREATEST(COALESCE(b.total_amount, 0) - COALESCE(b.paid_amount, 0), 0)",
                "b.onsite_amount_snapshot = CASE WHEN",
                "b.payment_status = 'DEPOSIT_FORFEITED'",
                "b.booking_status IN ('CANCELLED', 'NO_SHOW')");
    }
}
