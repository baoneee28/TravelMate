package com.travelmate.service;

import com.travelmate.dto.AdminRevenueSummaryDto;
import com.travelmate.dto.PartnerRevenueSummaryDto;
import com.travelmate.dto.RevenueItemDto;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * RevenueService — Tính doanh thu thật từ MySQL.
 *
 * Nguồn dữ liệu: bảng payments, chỉ lấy APPROVED + DEPOSIT_FORFEITED.
 *
 * Công thức (quy tắc cuối — chỉ tính trên tiền thu online):
 *   grossAmount     = payment.amount (tiền thực thu qua hệ thống: cọc 30% hoặc 100%)
 *   commissionBase  = payment.amount (luôn dùng số tiền online thực tế)
 *                     FULL_PAYMENT  → 100% đã thu qua hệ thống
 *                     DEPOSIT_30    → chỉ 30% cọc online (70% khách trả tại cơ sở không tính)
 *                     NO_SHOW       → cọc 30% bị giữ lại
 *   commission      = commissionBase × effectiveRate
 *   voucherDeduct   = booking.discountAmount nếu voucherCostBearer = PARTNER
 *   partnerNet      = max(0, grossAmount - commission - voucherDeduct)
 */
@Service
public class RevenueService {

    private final PaymentRepository paymentRepository;
    private final CommissionService commissionService;

    private static final List<PaymentStatus> REVENUE_STATUSES =
            List.of(PaymentStatus.APPROVED, PaymentStatus.DEPOSIT_FORFEITED);

    public RevenueService(PaymentRepository paymentRepository,
                          CommissionService commissionService) {
        this.paymentRepository = paymentRepository;
        this.commissionService = commissionService;
    }

    // ─── ADMIN ────────────────────────────────────────────────────────────────

    /**
     * Tổng hợp doanh thu toàn hệ thống cho Admin.
     */
    public AdminRevenueSummaryDto calculateAdminRevenueSummary() {
        List<Payment> payments = paymentRepository.findByPaymentStatusIn(REVENUE_STATUSES);
        AdminRevenueSummaryDto dto = new AdminRevenueSummaryDto();

        for (Payment p : payments) {
            BigDecimal gross = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;

            // v3: xác định cơ sở tính commission đúng theo nghiệp vụ
            Room room = p.getBooking().getRoom();
            BigDecimal commissionBase = resolveCommissionBase(p);
            BigDecimal commission = commissionService.calculateCommission(commissionBase, room);

            // Trừ voucher partner chịu để payout khớp bảng chi tiết
            BigDecimal voucherDeduct = BigDecimal.ZERO;
            if (p.getBooking().getVoucherCostBearer() == VoucherCostBearer.PARTNER
                    && p.getBooking().getDiscountAmount() != null) {
                voucherDeduct = p.getBooking().getDiscountAmount();
            }
            BigDecimal payout = gross.subtract(commission).subtract(voucherDeduct).max(BigDecimal.ZERO);

            dto.setTotalGross(dto.getTotalGross().add(gross));
            dto.setTotalCommissionBase(dto.getTotalCommissionBase().add(commissionBase));
            dto.setTotalCommission(dto.getTotalCommission().add(commission));
            dto.setTotalPayout(dto.getTotalPayout().add(payout));

            if (p.getPaymentStatus() == PaymentStatus.APPROVED) {
                dto.setTotalApproved(dto.getTotalApproved() + 1);
            } else {
                dto.setTotalForfeited(dto.getTotalForfeited() + 1);
            }
        }
        return dto;
    }

    /**
     * Danh sách chi tiết từng dòng doanh thu cho Admin.
     */
    public List<RevenueItemDto> getRevenueItemsForAdmin() {
        List<Payment> payments = paymentRepository.findByPaymentStatusIn(REVENUE_STATUSES);
        List<RevenueItemDto> items = new ArrayList<>();

        for (Payment p : payments) {
            items.add(buildRevenueItem(p));
        }
        return items;
    }

    // ─── PARTNER ──────────────────────────────────────────────────────────────

    /**
     * Tổng hợp doanh thu cho 1 partner cụ thể.
     * CHỈ lấy payment thuộc accommodation do partner đó sở hữu.
     */
    public PartnerRevenueSummaryDto calculatePartnerRevenueSummary(User partner) {
        List<Payment> payments = paymentRepository
                .findByBookingAccommodationOwnerAndPaymentStatusIn(partner, REVENUE_STATUSES);

        PartnerRevenueSummaryDto dto = new PartnerRevenueSummaryDto();
        dto.setPartnerName(partner.getName());

        // Lấy loại lưu trú của partner (1 partner 1 loại) — chỉ để hiển thị tổng quan
        PropertyType type = partner.getPartnerPropertyType();
        if (type != null) {
            dto.setPropertyType(type.name());
            dto.setCommissionRate(commissionService.getCommissionRate(type)
                    .multiply(BigDecimal.valueOf(100)));
        }

        for (Payment p : payments) {
            BigDecimal gross = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;

            // v3: xác định cơ sở tính commission đúng theo nghiệp vụ
            Room room = p.getBooking().getRoom();
            BigDecimal commissionBase = resolveCommissionBase(p);
            BigDecimal commission = commissionService.calculateCommission(commissionBase, room);

            // Voucher deduction: chỉ tính nếu partner chịu
            BigDecimal voucherDeduct = BigDecimal.ZERO;
            if (p.getBooking().getVoucherCostBearer() == VoucherCostBearer.PARTNER
                    && p.getBooking().getDiscountAmount() != null) {
                voucherDeduct = p.getBooking().getDiscountAmount();
            }

            BigDecimal payout = gross.subtract(commission).subtract(voucherDeduct).max(BigDecimal.ZERO);

            dto.setTotalGross(dto.getTotalGross().add(gross));
            dto.setTotalCommission(dto.getTotalCommission().add(commission));
            dto.setTotalVoucherDeduction(dto.getTotalVoucherDeduction().add(voucherDeduct));
            dto.setTotalPayout(dto.getTotalPayout().add(payout));
            dto.setTotalBookings(dto.getTotalBookings() + 1);
        }
        return dto;
    }

    /**
     * Danh sách chi tiết từng dòng doanh thu của 1 partner.
     */
    public List<RevenueItemDto> getRevenueItemsForPartner(User partner) {
        List<Payment> payments = paymentRepository
                .findByBookingAccommodationOwnerAndPaymentStatusIn(partner, REVENUE_STATUSES);

        List<RevenueItemDto> items = new ArrayList<>();
        for (Payment p : payments) {
            items.add(buildRevenueItem(p));
        }
        return items;
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    /**
     * Cơ sở tính commission = tiền thực thu qua hệ thống (payment.amount).
     * TravelMate chỉ tính commission trên tiền online, không tính 70% khách trả tại cơ sở.
     */
    private BigDecimal resolveCommissionBase(Payment p) {
        return p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
    }

    /**
     * Nhãn hiển thị cơ sở tính commission.
     */
    private String resolveCommissionBaseLabel(Payment p) {
        if (p.getPaymentOption() == PaymentOption.DEPOSIT_30) {
            if (p.getPaymentStatus() == PaymentStatus.DEPOSIT_FORFEITED) {
                return "Cọc 30% bị giữ (no-show)";
            }
            return "Cọc 30% đã thu online";
        }
        return "100% đã thu online";
    }

    /**
     * Chuyển 1 Payment → RevenueItemDto với đầy đủ thông tin tài chính.
     */
    private RevenueItemDto buildRevenueItem(Payment p) {
        RevenueItemDto item = new RevenueItemDto();

        // Booking info
        item.setBookingCode(p.getBooking().getBookingCode());
        item.setAccommodationName(p.getBooking().getAccommodation().getName());
        item.setRoomName(p.getBooking().getRoom().getRoomName());

        PropertyType type = p.getBooking().getAccommodation().getPropertyType();
        item.setPropertyType(type != null ? type.name() : "");

        // Room category
        Room room = p.getBooking().getRoom();
        if (room != null && room.getRoomCategory() != null) {
            item.setRoomCategory(room.getRoomCategory().name());
        } else {
            item.setRoomCategory("STANDARD");
        }

        User owner = p.getBooking().getAccommodation().getOwner();
        item.setPartnerName(owner != null ? owner.getName() : "N/A");

        // Payment info
        item.setPaymentOption(p.getPaymentOption() != null ? p.getPaymentOption().name() : "");
        item.setPaymentStatus(p.getPaymentStatus().name());

        // Financial breakdown — v3: dùng commissionBase đúng
        BigDecimal gross = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
        BigDecimal commissionBase = resolveCommissionBase(p);

        // Effective rate (thập phân)
        BigDecimal effectiveRate = commissionService.getEffectiveCommissionRate(room, type);
        BigDecimal commission = commissionService.calculateCommission(commissionBase, room);

        // Commission source
        boolean isOverride = commissionService.isRoomOverride(room);
        item.setCommissionSource(isOverride ? "ROOM_OVERRIDE" : "PROPERTY_TYPE_DEFAULT");

        BigDecimal voucherDeduct = BigDecimal.ZERO;
        if (p.getBooking().getVoucherCostBearer() == VoucherCostBearer.PARTNER
                && p.getBooking().getDiscountAmount() != null) {
            voucherDeduct = p.getBooking().getDiscountAmount();
        }

        // partnerNet = khoản admin chuyển lại partner (từ phần admin thu online), tối thiểu 0
        BigDecimal partnerNet = gross.subtract(commission).subtract(voucherDeduct).max(BigDecimal.ZERO);

        item.setGrossAmount(gross);
        item.setCommissionBase(commissionBase);
        item.setCommissionBaseLabel(resolveCommissionBaseLabel(p));
        item.setCommissionRate(effectiveRate);          // thập phân, backward compat
        item.setEffectiveCommissionRate(effectiveRate); // tường minh
        item.setCommissionAmount(commission);
        item.setVoucherDeductionAmount(voucherDeduct);
        item.setPartnerNetAmount(partnerNet);

        // Voucher
        item.setVoucherCode(p.getBooking().getVoucherCode());
        if (p.getBooking().getVoucherCostBearer() != null) {
            item.setVoucherCostBearer(p.getBooking().getVoucherCostBearer().name());
        }

        // Timestamp
        item.setCreatedAt(p.getBooking().getCreatedAt());

        return item;
    }
}
