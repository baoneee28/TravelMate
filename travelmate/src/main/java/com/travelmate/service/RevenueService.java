package com.travelmate.service;

import com.travelmate.dto.AdminRevenueSummaryDto;
import com.travelmate.dto.PartnerRevenueSummaryDto;
import com.travelmate.dto.RevenueItemDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * RevenueService — Tính doanh thu thật từ MySQL.
 *
 * Nguồn dữ liệu: bảng payments, chỉ lấy APPROVED + DEPOSIT_FORFEITED.
 *
 * Công thức:
 *   grossAmount     = payment.amount (tiền thực thu qua hệ thống: cọc 30% hoặc 100%)
 *   commissionBase  = tong don goc truoc voucher
 *   commission      = commissionBase x rate snapshot luc dat phong
 *   voucherDeduct   = booking.discountAmount nếu voucherCostBearer = PARTNER
 *   partnerNet      = grossAmount - commission - voucherDeduct
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
        List<Payment> payments = findOnlineRevenuePayments();
        AdminRevenueSummaryDto dto = new AdminRevenueSummaryDto();

        for (Payment p : payments) {
            BigDecimal gross = resolveOnlinePaid(p);

            BigDecimal commissionBase = resolveCommissionBase(p);
            BigDecimal commission = resolveCommissionAmount(p);

            // Trừ voucher partner chịu để payout khớp bảng chi tiết
            BigDecimal voucherDeduct = resolvePartnerVoucherAmount(p);
            BigDecimal payout = resolvePartnerPayout(p);

            dto.setTotalGross(dto.getTotalGross().add(gross));
            dto.setTotalOrderAmount(dto.getTotalOrderAmount().add(resolveTotalOrderAmount(p)));
            dto.setTotalOnsiteAmount(dto.getTotalOnsiteAmount().add(resolveOnsiteAmount(p)));
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
        List<Payment> payments = findOnlineRevenuePayments();
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
        List<Payment> payments = findManagedRevenuePaymentsForPartner(partner);

        PartnerRevenueSummaryDto dto = new PartnerRevenueSummaryDto();
        dto.setPartnerName(partner.getName());

        // Lấy loại lưu trú của partner (1 partner 1 loại) — chỉ để hiển thị tổng quan
        PropertyType type = partner.getPartnerPropertyType();
        if (type != null) {
            dto.setPropertyType(type.name());
            dto.setCommissionRate(commissionService.getCommissionRate(type)
                    .multiply(BigDecimal.valueOf(100)));
        }

        BigDecimal totalCommissionBase = BigDecimal.ZERO;
        for (Payment p : payments) {
            BigDecimal gross = resolveOnlinePaid(p);
            BigDecimal commissionBase = resolveCommissionBase(p);

            BigDecimal commission = resolveCommissionAmount(p);

            // Voucher deduction: chỉ tính nếu partner chịu
            BigDecimal voucherDeduct = resolvePartnerVoucherAmount(p);
            BigDecimal payout = resolvePartnerPayout(p);

            dto.setTotalGross(dto.getTotalGross().add(gross));
            dto.setTotalOrderAmount(dto.getTotalOrderAmount().add(resolveTotalOrderAmount(p)));
            dto.setTotalOnsiteAmount(dto.getTotalOnsiteAmount().add(resolveOnsiteAmount(p)));
            dto.setTotalCommission(dto.getTotalCommission().add(commission));
            dto.setTotalVoucherDeduction(dto.getTotalVoucherDeduction().add(voucherDeduct));
            dto.setTotalPayout(dto.getTotalPayout().add(payout));
            dto.setTotalBookings(dto.getTotalBookings() + 1);
            totalCommissionBase = totalCommissionBase.add(commissionBase);
        }

        if (totalCommissionBase.compareTo(BigDecimal.ZERO) > 0) {
            dto.setCommissionRate(dto.getTotalCommission()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalCommissionBase, 1, RoundingMode.HALF_UP));
        }
        return dto;
    }

    /**
     * Danh sách chi tiết từng dòng doanh thu của 1 partner.
     */
    public List<RevenueItemDto> getRevenueItemsForPartner(User partner) {
        List<Payment> payments = findManagedRevenuePaymentsForPartner(partner);

        List<RevenueItemDto> items = new ArrayList<>();
        for (Payment p : payments) {
            items.add(buildRevenueItem(p));
        }
        return items;
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private List<Payment> findManagedRevenuePaymentsForPartner(User partner) {
        return paymentRepository
                .findByBookingAccommodationOwnerAndPaymentStatusIn(partner, REVENUE_STATUSES)
                .stream()
                .filter(this::isOnlineRevenuePayment)
                .filter(payment -> payment.getBooking() != null
                        && isManagedByPartner(payment.getBooking().getAccommodation(), partner))
                .toList();
    }

    private List<Payment> findOnlineRevenuePayments() {
        return paymentRepository.findByPaymentStatusIn(REVENUE_STATUSES).stream()
                .filter(this::isOnlineRevenuePayment)
                .toList();
    }

    private boolean isOnlineRevenuePayment(Payment payment) {
        if (payment == null || payment.getBooking() == null) {
            return false;
        }
        BookingSource source = payment.getBooking().getBookingSource();
        return source == null || source == BookingSource.ONLINE;
    }

    private boolean isManagedByPartner(Accommodation accommodation, User partner) {
        if (accommodation == null || partner == null || partner.getId() == null) {
            return false;
        }
        if (partner.getPartnerPropertyType() == null || accommodation.getPropertyType() == null) {
            return false;
        }
        return accommodation.getOwner() != null
                && accommodation.getOwner().getId() != null
                && accommodation.getOwner().getId().equals(partner.getId())
                && accommodation.getPropertyType() == partner.getPartnerPropertyType();
    }

    private BigDecimal resolveCommissionBase(Payment p) {
        return resolvePreDiscountTotal(p);
    }

    private BigDecimal resolveCommissionAmount(Payment p) {
        BigDecimal base = resolveCommissionBase(p);
        if (hasFinancialSnapshot(p)) {
            return base.multiply(p.getBooking().getCommissionRateSnapshot())
                    .setScale(0, RoundingMode.HALF_UP);
        }
        return commissionService.calculateCommission(base, p.getBooking().getRoom());
    }

    private BigDecimal resolvePartnerVoucherAmount(Payment p) {
        if (hasFinancialSnapshot(p) && p.getBooking().getPartnerVoucherAmountSnapshot() != null) {
            return p.getBooking().getPartnerVoucherAmountSnapshot();
        }
        return p.getBooking().getVoucherCostBearer() == VoucherCostBearer.PARTNER
                && p.getBooking().getDiscountAmount() != null
                ? p.getBooking().getDiscountAmount() : BigDecimal.ZERO;
    }

    private BigDecimal resolvePartnerPayout(Payment p) {
        return resolveOnlinePaid(p).subtract(resolveCommissionAmount(p))
                .subtract(resolvePartnerVoucherAmount(p));
    }

    private BigDecimal resolveOnlinePaid(Payment p) {
        return p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
    }

    private BigDecimal resolveTotalOrderAmount(Payment p) {
        return resolvePreDiscountTotal(p);
    }

    private BigDecimal resolvePreDiscountTotal(Payment p) {
        return safePositive(p.getBooking().getTotalBeforeDiscount(),
                safePositive(p.getBooking().getTotalAmount(), resolveOnlinePaid(p)));
    }

    private BigDecimal safePositive(BigDecimal value, BigDecimal fallback) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : fallback;
    }

    private BigDecimal resolveOnsiteAmount(Payment p) {
        if (hasFinancialSnapshot(p) && p.getBooking().getOnsiteAmountSnapshot() != null) {
            return p.getBooking().getOnsiteAmountSnapshot();
        }
        if (p.getPaymentOption() == PaymentOption.DEPOSIT_30
                && p.getPaymentStatus() != PaymentStatus.DEPOSIT_FORFEITED) {
            return p.getBooking().getRemainingAmount() != null
                    ? p.getBooking().getRemainingAmount() : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    private boolean hasFinancialSnapshot(Payment p) {
        return p != null && p.getBooking() != null && p.getBooking().getCommissionRateSnapshot() != null;
    }

    /**
     * Nhãn hiển thị cơ sở tính commission.
     */
    private String resolveCommissionBaseLabel(Payment p) {
        if (p.getPaymentOption() == PaymentOption.DEPOSIT_30) {
            if (p.getPaymentStatus() == PaymentStatus.DEPOSIT_FORFEITED) {
                return "Tổng đơn gốc (cọc bị giữ)";
            }
            return "Tổng đơn gốc (cọc 30%)";
        }
        return "Tổng đơn gốc (thanh toán 100%)";
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

        // Financial breakdown: dùng số tiền và rate snapshot của booking.
        BigDecimal gross = resolveOnlinePaid(p);
        BigDecimal commissionBase = resolveCommissionBase(p);

        BigDecimal effectiveRate = hasFinancialSnapshot(p)
                ? p.getBooking().getCommissionRateSnapshot()
                : commissionService.getEffectiveCommissionRate(room, type);
        BigDecimal commission = resolveCommissionAmount(p);

        String source = hasFinancialSnapshot(p) ? p.getBooking().getCommissionSourceSnapshot()
                : (commissionService.isRoomOverride(room) ? "ROOM_OVERRIDE" : "PROPERTY_TYPE_DEFAULT");
        item.setCommissionSource(source);

        BigDecimal voucherDeduct = resolvePartnerVoucherAmount(p);
        BigDecimal partnerNet = resolvePartnerPayout(p);

        item.setTotalOrderAmount(resolveTotalOrderAmount(p));
        item.setGrossAmount(gross);
        item.setOnsiteAmount(resolveOnsiteAmount(p));
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
