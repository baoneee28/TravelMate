package com.travelmate.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("travelmate_db.sql — Dữ liệu khởi tạo User Portal")
class UserPortalSqlSeedTest {

    private static final Path DB_SQL = Path.of("src/main/resources/travelmate_db.sql");
    private static final Path DB_HEALTH_CHECKS = Path.of("docs/sql/travelmate_db_health_checks.sql");

    @Test
    @DisplayName("SQL gốc có bảng ảnh phòng và gắn voucher kho Partner")
    void sqlContainsRoomImagesAndVoucherAssignmentTables() throws IOException {
        String sql = Files.readString(DB_SQL);

        assertThat(sql).contains("CREATE TABLE password_reset_tokens");
        assertThat(sql).contains("CREATE TABLE room_images");
        assertThat(sql).contains("CREATE TABLE room_voucher_assignments");
        assertThat(sql).contains("available_for_booking   TINYINT(1)    NOT NULL DEFAULT 1");
        assertThat(sql).contains("INSERT INTO room_voucher_assignments");
        assertThat(sql).contains("INSERT INTO room_images (room_id, image_url, caption, sort_order, is_primary");
    }

    @Test
    @DisplayName("Dữ liệu voucher dùng mô hình Admin phát hành, Partner gắn phòng/căn")
    void sqlSeedsAdminIssuedPartnerRoomVouchers() throws IOException {
        String sql = Files.readString(DB_SQL);

        assertThat(sql).contains("'USER_GLOBAL', 'ADMIN'");
        assertThat(sql).contains("'PARTNER_ROOM', 'HOTEL', 'PARTNER'");
        assertThat(sql).contains("'PARTNER_ROOM', 'RESORT', 'PARTNER'");
        assertThat(sql).contains("'PARTNER_ROOM', 'VILLA', 'PARTNER'");
        assertThat(sql).contains("'PARTNER_ROOM', 'HOMESTAY', 'PARTNER'");
        assertThat(sql).contains("'PARTNER_ROOM', NULL, 'ADMIN'");
        assertThat(sql).contains("STAYFLEX100");
        assertThat(sql).contains("property_type");
        assertThat(sql).contains("Admin phát hành");
        assertThat(sql).contains("room_voucher_assignments");
        assertThat(sql).contains("v.code = 'LATA20'", "v.code = 'ANAM15'");
        assertThat(sql).doesNotContain(
                "Voucher do Partner",
                "'PARTNER_ACCOMMODATION'",
                "các đối tác có thể tạo voucher giảm giá riêng",
                "Muốn tạo voucher nhưng không thấy Cơ sở lưu trú của tôi",
                "Voucher theo cơ sở");
    }

    @Test
    @DisplayName("Dữ liệu vận hành giữ DIRECT ngoài thanh toán TravelMate và ghi đúng loại Partner")
    void sqlSeedsDirectBookingWithoutGatewayPaymentAndNaturalPartnerType() throws IOException {
        String sql = Files.readString(DB_SQL);

        assertThat(sql).contains("'Homestay Rừng Thông Đà Lạt', 'Homestay Rừng Thông Đà Lạt', '0908 555 666', 'PARTNER', 'ACTIVE', 'HOMESTAY'");
        assertThat(sql).contains("BK-PL-DIRECT-001 thu trực tiếp tại quầy: không tạo payment qua TravelMate");
        assertThat(sql).doesNotContain("'TXN-PL-DIRECT-001'");
    }

    @Test
    @DisplayName("Dữ liệu VNPAY success chuyển sang CONFIRMED và chờ đối tác giữ phòng")
    void sqlSeedsVnpaySuccessAsConfirmedPendingPartner() throws IOException {
        String sql = Files.readString(DB_SQL);

        assertThat(sql).contains("'CONFIRMED', 'FULL_PAYMENT', 'APPROVED', 'PENDING_PARTNER_CONFIRMATION'");
        assertThat(sql).contains("'CONFIRMED', 'DEPOSIT_30', 'APPROVED', 'PENDING_PARTNER_CONFIRMATION'");
        assertThat(sql).contains("VNPAY xác nhận thành công");
        assertThat(sql).doesNotContain("Chờ Admin duyệt " + "thanh toán");
    }

    @Test
    @DisplayName("SQL gốc đủ cột Booking/Payment cho luồng VNPAY mới")
    void sqlSchemaContainsBookingAndPaymentGatewayColumns() throws IOException {
        String sql = Files.readString(DB_SQL);

        assertThat(sql).contains("CREATE TABLE bookings");
        assertThat(sql).contains(
                "expire_at",
                "commission_rate_snapshot",
                "commission_source_snapshot",
                "commission_base_amount",
                "commission_amount_snapshot",
                "online_paid_amount_snapshot",
                "onsite_amount_snapshot",
                "partner_payout_snapshot",
                "refund_amount",
                "cancellation_fee");
        assertThat(sql).contains("CREATE TABLE payments");
        assertThat(sql).contains(
                "gateway",
                "vnp_txn_ref",
                "vnp_transaction_no",
                "vnp_bank_code",
                "vnp_bank_tran_no",
                "vnp_card_type",
                "vnp_response_code",
                "vnp_transaction_status",
                "vnp_pay_date",
                "raw_return_payload",
                "raw_ipn_payload",
                "confirmed_from_gateway_at");
    }

    @Test
    @DisplayName("SQL gốc có unique index chống trùng VNPAY, settlement và ví")
    void sqlSchemaContainsIdempotencyIndexes() throws IOException {
        String sql = Files.readString(DB_SQL).toLowerCase(Locale.ROOT);

        assertThat(Pattern.compile("vnp_txn_ref\\s+varchar\\(100\\)\\s+default null unique")
                .matcher(sql).find()).isTrue();
        assertThat(sql).contains(
                "unique key uk_settlement_partner_period",
                "unique key uk_wallet_tx_settlement_credit",
                "unique key uk_wallet_tx_withdrawal_type");
    }

    @Test
    @DisplayName("Settlement trong SQL là kỳ tháng và đúng công thức payout")
    void sqlSeedUsesMonthlySettlementAndCorrectPayoutFormula() throws IOException {
        String sql = Files.readString(DB_SQL);
        Pattern pattern = Pattern.compile(
                "\\(\\s*\\d+\\s*,\\s*'(\\d{4}-\\d{2}-\\d{2})'\\s*,\\s*'(\\d{4}-\\d{2}-\\d{2})'\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)",
                Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(sql);
        int total = 0;

        while (matcher.find()) {
            LocalDate start = LocalDate.parse(matcher.group(1));
            LocalDate end = LocalDate.parse(matcher.group(2));
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            long gross = Long.parseLong(matcher.group(3));
            long commission = Long.parseLong(matcher.group(4));
            long voucherDeduction = Long.parseLong(matcher.group(5));
            long payout = Long.parseLong(matcher.group(6));

            assertThat(days).isGreaterThanOrEqualTo(28);
            assertThat(payout).isEqualTo(gross - commission - voucherDeduction);
            total++;
        }

        assertThat(total).isGreaterThan(0);
    }

    @Test
    @DisplayName("SQL gốc không lộ wording nhạy cảm trên dữ liệu hiển thị")
    void sqlSeedDoesNotExposeSensitiveImplementationText() throws IOException {
        String sql = Files.readString(DB_SQL).toLowerCase(Locale.ROOT);

        assertThat(sql).doesNotContain(
                "ai seed",
                "chatgpt",
                "giảng viên thấy",
                "thầy thấy",
                "seed để",
                "demo để",
                "dữ liệu giả",
                "test alias",
                "bk-seed",
                "wd-seed",
                "vnpay_demo",
                "bk-demo",
                "wd-demo",
                "txn-demo",
                "demo seed",
                "seed data",
                "partner wallet demo",
                "thanh toán demo",
                "demo:",
                "demo listing",
                "cal demo");
    }

    @Test
    @DisplayName("Có file query kiểm tra DB sau import để đối chiếu khi trình bày")
    void sqlHealthCheckFileDocumentsManualDbVerification() throws IOException {
        String sql = Files.readString(DB_HEALTH_CHECKS);

        assertThat(sql).contains(
                "SHOW COLUMNS FROM bookings LIKE 'expire_at'",
                "SHOW COLUMNS FROM payments LIKE 'vnp_txn_ref'",
                "partner_wallet_transactions",
                "voucher_deduction_amount",
                "expected_payout");
    }
}
