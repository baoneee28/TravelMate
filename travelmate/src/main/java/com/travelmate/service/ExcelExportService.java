package com.travelmate.service;

import com.travelmate.dto.SettlementDetailItemDto;
import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.PartnerWithdrawalRequest;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.PartnerWithdrawalStatus;
import com.travelmate.entity.enums.SettlementStatus;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] exportSettlementDetail(PartnerSettlement settlement, List<SettlementDetailItemDto> breakdown) {
        if (settlement == null) {
            throw new IllegalArgumentException("Settlement không hợp lệ!");
        }

        try (SXSSFWorkbook workbook = createStreamingWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            WorkbookStyles styles = createStyles(workbook);
            List<SettlementDetailItemDto> safeBreakdown = breakdown != null ? breakdown : List.of();

            Sheet summarySheet = createSheet(workbook, "Tong quan");
            writeSettlementSummary(summarySheet, settlement, safeBreakdown, styles);

            Sheet detailSheet = createSheet(workbook, "Chi tiet booking");
            writeSettlementBreakdown(detailSheet, safeBreakdown, styles);

            workbook.write(outputStream);
            workbook.dispose();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo file Excel quyết toán!", e);
        }
    }

    public byte[] exportWithdrawals(List<PartnerWithdrawalRequest> withdrawals) {
        try (SXSSFWorkbook workbook = createStreamingWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            WorkbookStyles styles = createStyles(workbook);
            Sheet sheet = createSheet(workbook, "Yeu cau rut tien");

            writeTitle(sheet, "BÁO CÁO YÊU CẦU RÚT TIỀN PARTNER", styles.title());
            writeGeneratedAt(sheet, 1, styles.muted());

            int rowIndex = 3;
            Row header = sheet.createRow(rowIndex++);
            String[] headers = {
                    "Mã yêu cầu", "Partner", "Số tiền", "Ngân hàng", "Số tài khoản",
                    "Chủ tài khoản", "Trạng thái", "Ngày yêu cầu", "Ngày xử lý", "Ghi chú Admin"
            };
            writeHeader(header, headers, styles.header());

            for (PartnerWithdrawalRequest withdrawal : withdrawals != null ? withdrawals : List.<PartnerWithdrawalRequest>of()) {
                Row row = sheet.createRow(rowIndex++);
                writeTextCell(row, 0, safe(withdrawal.getRequestCode()), styles.text());
                writeTextCell(row, 1, displayUser(withdrawal.getPartner()), styles.text());
                writeMoneyCell(row, 2, withdrawal.getAmount(), styles.money());
                writeTextCell(row, 3, safe(withdrawal.getBankName()), styles.text());
                writeTextCell(row, 4, maskedBankAccount(withdrawal), styles.text());
                writeTextCell(row, 5, safe(withdrawal.getBankAccountHolder()), styles.text());
                writeTextCell(row, 6, withdrawalStatusDisplay(withdrawal.getWithdrawalStatus()),
                        withdrawalStatusStyle(withdrawal.getWithdrawalStatus(), styles));
                writeTextCell(row, 7, formatDateTime(withdrawal.getRequestedAt()), styles.text());
                writeTextCell(row, 8, formatDateTime(withdrawal.getProcessedAt()), styles.text());
                writeTextCell(row, 9, safe(withdrawal.getAdminNote()), styles.text());
            }

            sheet.createFreezePane(0, 4);
            autoSizeColumns(sheet, headers.length);
            workbook.write(outputStream);
            workbook.dispose();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không thể tạo file Excel yêu cầu rút tiền!", e);
        }
    }

    private SXSSFWorkbook createStreamingWorkbook() {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        workbook.setCompressTempFiles(true);
        return workbook;
    }

    private Sheet createSheet(Workbook workbook, String name) {
        Sheet sheet = workbook.createSheet(name);
        if (sheet instanceof SXSSFSheet streamingSheet) {
            streamingSheet.trackAllColumnsForAutoSizing();
        }
        return sheet;
    }

    private void writeSettlementSummary(Sheet sheet, PartnerSettlement settlement,
                                        List<SettlementDetailItemDto> breakdown,
                                        WorkbookStyles styles) {
        writeTitle(sheet, "BÁO CÁO QUYẾT TOÁN PARTNER", styles.title());
        writeGeneratedAt(sheet, 1, styles.muted());

        int rowIndex = 3;
        rowIndex = writeKeyValue(sheet, rowIndex, "Mã settlement", settlementCode(settlement), styles);
        rowIndex = writeKeyValue(sheet, rowIndex, "Partner", displayUser(settlement.getPartner()), styles);
        rowIndex = writeKeyValue(sheet, rowIndex, "Kỳ quyết toán", formatPeriod(settlement), styles);
        rowIndex = writeKeyValue(sheet, rowIndex, "Ngày chi trả dự kiến", formatDate(settlement.getScheduledPayoutDate()), styles);
        rowIndex = writeKeyValue(sheet, rowIndex, "Trạng thái", settlementStatusDisplay(settlement.getSettlementStatus()), styles);
        rowIndex = writeKeyValue(sheet, rowIndex, "Ngày thanh toán", formatDateTime(settlement.getSettlementDate()), styles);
        rowIndex = writeKeyValue(sheet, rowIndex, "Ghi chú", safe(settlement.getNote()), styles);

        rowIndex += 1;
        rowIndex = writeMoneyKeyValue(sheet, rowIndex, "Gross", sumBreakdown(breakdown, "gross"), styles);
        rowIndex = writeMoneyKeyValue(sheet, rowIndex, "Commission", sumBreakdown(breakdown, "commission"), styles);
        rowIndex = writeMoneyKeyValue(sheet, rowIndex, "Voucher Partner chịu", sumBreakdown(breakdown, "voucher"), styles);
        writeMoneyKeyValue(sheet, rowIndex, "Payout", sumBreakdown(breakdown, "payout"), styles);

        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 42 * 256);
        sheet.setColumnWidth(2, 18 * 256);
        sheet.setColumnWidth(3, 18 * 256);
    }

    private BigDecimal sumBreakdown(List<SettlementDetailItemDto> breakdown, String field) {
        BigDecimal total = BigDecimal.ZERO;
        for (SettlementDetailItemDto item : breakdown != null ? breakdown : List.<SettlementDetailItemDto>of()) {
            BigDecimal value = switch (field) {
                case "gross" -> item.getGross();
                case "commission" -> item.getCommissionAmount();
                case "voucher" -> item.getVoucherDeductAmount();
                case "payout" -> item.getPartnerPayout();
                default -> BigDecimal.ZERO;
            };
            total = total.add(zeroIfNull(value));
        }
        return total;
    }

    private void writeSettlementBreakdown(Sheet sheet, List<SettlementDetailItemDto> breakdown, WorkbookStyles styles) {
        writeTitle(sheet, "CHI TIẾT BOOKING TRONG KỲ", styles.title());
        writeGeneratedAt(sheet, 1, styles.muted());

        int rowIndex = 3;
        Row header = sheet.createRow(rowIndex++);
        String[] headers = {
                "Mã booking", "Cơ sở", "Phòng", "Check-in", "Check-out",
                "Gross", "Tỷ lệ HH", "Commission", "Voucher", "Voucher trừ",
                "Đối tác nhận", "Trạng thái thanh toán"
        };
        writeHeader(header, headers, styles.header());

        for (SettlementDetailItemDto item : breakdown) {
            Row row = sheet.createRow(rowIndex++);
            writeTextCell(row, 0, safe(item.getBookingCode()), styles.text());
            writeTextCell(row, 1, safe(item.getAccName()), styles.text());
            writeTextCell(row, 2, safe(item.getRoomName()), styles.text());
            writeTextCell(row, 3, safe(item.getCheckIn()), styles.text());
            writeTextCell(row, 4, safe(item.getCheckOut()), styles.text());
            writeMoneyCell(row, 5, item.getGross(), styles.money());
            writeTextCell(row, 6, safe(item.getCommissionRateDisplay()), styles.text());
            writeMoneyCell(row, 7, item.getCommissionAmount(), styles.money());
            writeTextCell(row, 8, safe(item.getVoucherCode()), styles.text());
            writeMoneyCell(row, 9, item.getVoucherDeductAmount(), styles.money());
            writeMoneyCell(row, 10, item.getPartnerPayout(), styles.money());
            writeTextCell(row, 11, safe(item.getPaymentStatusVN()), styles.statusPaid());
        }

        sheet.createFreezePane(0, 4);
        autoSizeColumns(sheet, headers.length);
    }

    private void writeTitle(Sheet sheet, String title, CellStyle titleStyle) {
        Row row = sheet.createRow(0);
        row.setHeightInPoints(28);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
    }

    private void writeGeneratedAt(Sheet sheet, int rowIndex, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue("Xuất lúc: " + LocalDateTime.now().format(DATE_TIME_FORMATTER));
        cell.setCellStyle(style);
    }

    private int writeKeyValue(Sheet sheet, int rowIndex, String label, String value, WorkbookStyles styles) {
        Row row = sheet.createRow(rowIndex);
        writeTextCell(row, 0, label, styles.label());
        writeTextCell(row, 1, value, styles.text());
        return rowIndex + 1;
    }

    private int writeMoneyKeyValue(Sheet sheet, int rowIndex, String label, BigDecimal value, WorkbookStyles styles) {
        Row row = sheet.createRow(rowIndex);
        writeTextCell(row, 0, label, styles.label());
        writeMoneyCell(row, 1, value, styles.money());
        return rowIndex + 1;
    }

    private void writeHeader(Row row, String[] headers, CellStyle style) {
        for (int i = 0; i < headers.length; i++) {
            writeTextCell(row, i, headers[i], style);
        }
    }

    private void writeTextCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeMoneyCell(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(zeroIfNull(value).doubleValue());
        cell.setCellStyle(style);
    }

    private WorkbookStyles createStyles(Workbook workbook) {
        DataFormat dataFormat = workbook.createDataFormat();

        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle title = workbook.createCellStyle();
        title.setFont(titleFont);
        title.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        title.setAlignment(HorizontalAlignment.LEFT);
        title.setVerticalAlignment(VerticalAlignment.CENTER);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle header = bordered(workbook);
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);

        Font labelFont = workbook.createFont();
        labelFont.setBold(true);

        CellStyle label = bordered(workbook);
        label.setFont(labelFont);
        label.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        label.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle text = bordered(workbook);
        text.setWrapText(true);
        text.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle money = bordered(workbook);
        money.setDataFormat(dataFormat.getFormat("#,##0"));
        money.setAlignment(HorizontalAlignment.RIGHT);

        CellStyle statusPaid = filledStatus(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle statusPending = filledStatus(workbook, IndexedColors.LIGHT_YELLOW);
        CellStyle statusRejected = filledStatus(workbook, IndexedColors.ROSE);

        Font mutedFont = workbook.createFont();
        mutedFont.setItalic(true);
        mutedFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        CellStyle muted = workbook.createCellStyle();
        muted.setFont(mutedFont);

        return new WorkbookStyles(title, header, label, text, money,
                statusPaid, statusPending, statusRejected, muted);
    }

    private CellStyle bordered(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle filledStatus(Workbook workbook, IndexedColors color) {
        CellStyle style = bordered(workbook);
        style.setWrapText(true);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void autoSizeColumns(Sheet sheet, int totalColumns) {
        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(currentWidth + 900, 12 * 256), 42 * 256));
        }
    }

    private String settlementCode(PartnerSettlement settlement) {
        return settlement.getId() != null ? String.format("STL-%06d", settlement.getId()) : "STL-DEMO";
    }

    private String displayUser(User user) {
        if (user == null) {
            return "—";
        }
        if (hasText(user.getName())) {
            return user.getName();
        }
        return safe(user.getEmail());
    }

    private String formatPeriod(PartnerSettlement settlement) {
        return formatDate(settlement.getPeriodStart()) + " - " + formatDate(settlement.getPeriodEnd());
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "—";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "—";
    }

    private String settlementStatusDisplay(SettlementStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case PENDING -> "Chờ chi trả";
            case PAID -> "Đã ghi nhận chi trả";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String withdrawalStatusDisplay(PartnerWithdrawalStatus status) {
        return status != null ? status.getDisplayName() : "—";
    }

    private CellStyle withdrawalStatusStyle(PartnerWithdrawalStatus status, WorkbookStyles styles) {
        if (status == null) {
            return styles.text();
        }
        return switch (status) {
            case PENDING -> styles.statusPending();
            case PAID -> styles.statusPaid();
            case REJECTED -> styles.statusRejected();
        };
    }

    private String maskedBankAccount(PartnerWithdrawalRequest withdrawal) {
        if (withdrawal == null) {
            return "—";
        }
        return withdrawal.getMaskedBankAccountNumber();
    }

    private String safe(String value) {
        return hasText(value) ? value.trim() : "—";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record WorkbookStyles(
            CellStyle title,
            CellStyle header,
            CellStyle label,
            CellStyle text,
            CellStyle money,
            CellStyle statusPaid,
            CellStyle statusPending,
            CellStyle statusRejected,
            CellStyle muted
    ) {
    }
}
