package com.travelmate.service;

import com.travelmate.dto.SettlementDetailItemDto;
import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.PartnerWithdrawalRequest;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.PartnerWithdrawalStatus;
import com.travelmate.entity.enums.SettlementStatus;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExcelExportService — Xuất Excel đối soát")
class ExcelExportServiceTest {

    private final ExcelExportService excelExportService = new ExcelExportService();

    @Test
    @DisplayName("Settlement detail export trả về XLSX bytes")
    void exportSettlementDetail_shouldReturnXlsxBytes() {
        byte[] bytes = excelExportService.exportSettlementDetail(sampleSettlement(), List.of(sampleBreakdownItem()));

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }

    @Test
    @DisplayName("Settlement detail export có đúng 2 sheet")
    void exportSettlementDetail_shouldContainSheets() throws Exception {
        byte[] bytes = excelExportService.exportSettlementDetail(sampleSettlement(), List.of(sampleBreakdownItem()));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getSheet("Tong quan")).isNotNull();
            assertThat(workbook.getSheet("Chi tiet booking")).isNotNull();
        }
    }

    @Test
    @DisplayName("Withdrawal export trả về XLSX bytes")
    void exportWithdrawals_shouldReturnXlsxBytes() {
        byte[] bytes = excelExportService.exportWithdrawals(List.of(sampleWithdrawal()));

        assertThat(bytes).isNotEmpty();
        assertThat(bytes[0]).isEqualTo((byte) 'P');
        assertThat(bytes[1]).isEqualTo((byte) 'K');
    }

    @Test
    @DisplayName("Withdrawal export có header chính")
    void exportWithdrawals_shouldContainHeaders() throws Exception {
        byte[] bytes = excelExportService.exportWithdrawals(List.of(sampleWithdrawal()));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            String text = sheetText(workbook.getSheet("Yeu cau rut tien"));
            assertThat(text).contains("Mã yêu cầu");
            assertThat(text).contains("Partner");
            assertThat(text).contains("Số tiền");
            assertThat(text).contains("Ngân hàng");
            assertThat(text).contains("Ghi chú Admin");
        }
    }

    @Test
    @DisplayName("Withdrawal export mask số tài khoản")
    void exportWithdrawals_shouldMaskBankAccount() throws Exception {
        byte[] bytes = excelExportService.exportWithdrawals(List.of(sampleWithdrawal()));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            String text = sheetText(workbook.getSheet("Yeu cau rut tien"));
            assertThat(text).contains("****6789");
            assertThat(text).doesNotContain("0123456789");
        }
    }

    private PartnerSettlement sampleSettlement() {
        PartnerSettlement settlement = new PartnerSettlement();
        settlement.setId(1L);
        settlement.setPartner(samplePartner());
        settlement.setPeriodStart(LocalDate.of(2026, 4, 1));
        settlement.setPeriodEnd(LocalDate.of(2026, 4, 30));
        settlement.setScheduledPayoutDate(LocalDate.of(2026, 5, 10));
        settlement.setSettlementStatus(SettlementStatus.PAID);
        settlement.setSettlementDate(LocalDateTime.of(2026, 5, 10, 9, 30));
        settlement.setGrossAmount(new BigDecimal("5000000"));
        settlement.setCommissionAmount(new BigDecimal("750000"));
        settlement.setVoucherDeductionAmount(new BigDecimal("100000"));
        settlement.setPayoutAmount(new BigDecimal("4150000"));
        settlement.setNote("Admin đã chuyển khoản.");
        return settlement;
    }

    private SettlementDetailItemDto sampleBreakdownItem() {
        SettlementDetailItemDto item = new SettlementDetailItemDto();
        item.setBookingCode("BK-DEMO-001");
        item.setAccName("LATA Hotel & Apartments");
        item.setRoomName("Phòng Suite Cao Cấp");
        item.setCheckIn("20/04/2026");
        item.setCheckOut("22/04/2026");
        item.setGross(new BigDecimal("3600000"));
        item.setCommissionRateDisplay("15% (mặc định), CK trên 100% đã thu");
        item.setCommissionAmount(new BigDecimal("540000"));
        item.setVoucherCode("LATA20");
        item.setVoucherDeductAmount(new BigDecimal("100000"));
        item.setPartnerPayout(new BigDecimal("2960000"));
        item.setPaymentStatusVN("Đã thanh toán");
        return item;
    }

    private PartnerWithdrawalRequest sampleWithdrawal() {
        PartnerWithdrawalRequest request = new PartnerWithdrawalRequest();
        request.setId(3L);
        request.setPartner(samplePartner());
        request.setRequestCode("WD-DEMO-001");
        request.setAmount(new BigDecimal("1000000"));
        request.setBankName("Vietcombank");
        request.setBankAccountNumber("0123456789");
        request.setBankAccountHolder("NGUYEN VAN PARTNER");
        request.setBankBranch("CN Đà Lạt");
        request.setWithdrawalStatus(PartnerWithdrawalStatus.PENDING);
        request.setRequestedAt(LocalDateTime.of(2026, 5, 22, 10, 15));
        request.setAdminNote("Demo đối soát nội bộ.");
        return request;
    }

    private User samplePartner() {
        User partner = new User();
        partner.setId(9L);
        partner.setName("Partner Demo");
        partner.setEmail("partner@travelmate.vn");
        partner.setRole(User.Role.PARTNER);
        return partner;
    }

    private String sheetText(Sheet sheet) {
        StringBuilder text = new StringBuilder();
        for (Row row : sheet) {
            for (Cell cell : row) {
                text.append(cell.toString()).append(' ');
            }
        }
        return text.toString();
    }
}
