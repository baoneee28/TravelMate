package com.travelmate.service;

import com.travelmate.dto.SettlementDetailItemDto;
import com.travelmate.entity.Booking;
import com.travelmate.entity.PartnerSettlement;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.entity.enums.SettlementStatus;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.repository.PartnerSettlementRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * SettlementService — Quản lý quyết toán THÁNG cho partner.
 *
 * Luồng:
 *  1. Admin bấm "Tạo quyết toán tháng trước":
 *     → Tính tất cả Payment đủ điều kiện quyết toán trong tháng trước
 *     → Group by partner, tạo PartnerSettlement PENDING
 *     → Tránh tạo trùng (check exists)
 *  2. Admin bấm "Đánh dấu đã TT" → PAID
 *
 * Kỳ quyết toán: Ngày 01 → cuối tháng trước.
 *
 * Điều kiện đưa booking vào quyết toán (isSettlementEligible):
 *  - Booking ONLINE + Payment APPROVED + BookingStatus COMPLETED
 *  - Booking ONLINE + Payment DEPOSIT_FORFEITED + BookingStatus NO_SHOW
 *  → Chỉ booking đã hoàn tất lưu trú hoặc khách không đến mới được quyết toán.
 *
 * Quy tắc commission: chỉ tính trên tiền thực thu online (payment.amount).
 *   - DEPOSIT_30 + APPROVED: commBase = cọc 30% đã thu online
 *   - DEPOSIT_30 + DEPOSIT_FORFEITED (no-show): commBase = cọc 30% bị giữ
 *   - FULL_PAYMENT: commBase = 100% đã thu
 *   payout = max(0, gross - commission - voucherPartnerDeduct)
 */
@SuppressWarnings("null")
@Service
public class SettlementService {

    private final PartnerSettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CommissionService commissionService;

    private static final List<PaymentStatus> REVENUE_STATUSES =
            List.of(PaymentStatus.APPROVED, PaymentStatus.DEPOSIT_FORFEITED);

    public SettlementService(PartnerSettlementRepository settlementRepository,
                             PaymentRepository paymentRepository,
                             UserRepository userRepository,
                             CommissionService commissionService) {
        this.settlementRepository = settlementRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.commissionService = commissionService;
    }

    // ─── GENERATE ─────────────────────────────────────────────────────────────

    /**
     * Tạo settlement cho TẤT CẢ partner trong THÁNG trước.
     * Admin bấm "Tạo quyết toán tháng trước" → gọi hàm này.
     *
     * @return Danh sách settlement mới tạo (bỏ qua partner đã có settlement trong kỳ)
     */
    @Transactional
    public List<PartnerSettlement> generateMonthlySettlements() {
        // Xác định kỳ quyết toán: ngày 01 → cuối tháng trước
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate periodStart = lastMonth.atDay(1);
        LocalDate periodEnd   = lastMonth.atEndOfMonth();

        // Lấy tất cả payment APPROVED/DEPOSIT_FORFEITED
        List<Payment> allPayments = paymentRepository.findByPaymentStatusIn(REVENUE_STATUSES);

        // Lọc payment đủ điều kiện quyết toán VÀ nằm trong kỳ tháng trước
        List<Payment> periodPayments = allPayments.stream()
                .filter(this::isSettlementEligible)
                .filter(p -> {
                    LocalDate paymentDate = resolveSettlementDate(p);
                    return !paymentDate.isBefore(periodStart) && !paymentDate.isAfter(periodEnd);
                })
                .toList();

        // Lấy tất cả partner
        List<User> partners = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.Role.PARTNER)
                .toList();

        List<PartnerSettlement> created = new ArrayList<>();

        for (User partner : partners) {
            // Tránh tạo trùng
            if (settlementRepository.existsByPartnerAndPeriodStartAndPeriodEnd(
                    partner, periodStart, periodEnd)) {
                continue;
            }

            // Lọc payment của partner này trong kỳ
            List<Payment> partnerPayments = periodPayments.stream()
                    .filter(p -> {
                        User owner = p.getBooking().getAccommodation().getOwner();
                        return owner != null && owner.getId().equals(partner.getId());
                    })
                    .toList();

            if (partnerPayments.isEmpty()) continue; // Không có doanh thu → không tạo

            // Tính toán
            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal commission = BigDecimal.ZERO;
            BigDecimal voucherDeduct = BigDecimal.ZERO;

            for (Payment p : partnerPayments) {
                BigDecimal g = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;

                Room room = p.getBooking().getRoom();
                BigDecimal commBase = resolveCommissionBase(p);
                BigDecimal c = commissionService.calculateCommission(commBase, room);

                BigDecimal vd = BigDecimal.ZERO;
                if (p.getBooking().getVoucherCostBearer() == VoucherCostBearer.PARTNER
                        && p.getBooking().getDiscountAmount() != null) {
                    vd = p.getBooking().getDiscountAmount();
                }

                gross = gross.add(g);
                commission = commission.add(c);
                voucherDeduct = voucherDeduct.add(vd);
            }

            BigDecimal payout = gross.subtract(commission).subtract(voucherDeduct).max(BigDecimal.ZERO);

            PartnerSettlement settlement = new PartnerSettlement();
            settlement.setPartner(partner);
            settlement.setPeriodStart(periodStart);
            settlement.setPeriodEnd(periodEnd);
            settlement.setGrossAmount(gross);
            settlement.setCommissionAmount(commission);
            settlement.setVoucherDeductionAmount(voucherDeduct);
            settlement.setPayoutAmount(payout);
            settlement.setSettlementStatus(SettlementStatus.PENDING);

            created.add(settlementRepository.save(settlement));
        }

        return created;
    }

    // ─── ELIGIBILITY ─────────────────────────────────────────────────────────

    /**
     * Kiểm tra 1 payment có đủ điều kiện đưa vào quyết toán hay không.
     *
     * Chỉ chấp nhận:
     * - ONLINE + APPROVED + COMPLETED  → khách đã hoàn tất lưu trú
     * - ONLINE + DEPOSIT_FORFEITED + NO_SHOW → khách không đến, cọc bị giữ
     *
     * Loại bỏ: PENDING_ADMIN_APPROVAL, CONFIRMED, CHECKED_IN, CANCELLED, REFUNDED,
     *           DIRECT, MANUAL_BLOCK...
     */
    private boolean isSettlementEligible(Payment p) {
        if (p == null || p.getBooking() == null) return false;

        Booking b = p.getBooking();

        // Chỉ booking ONLINE mới quyết toán
        if (b.getBookingSource() != null && b.getBookingSource() != BookingSource.ONLINE) {
            return false;
        }

        PaymentStatus ps = p.getPaymentStatus();
        BookingStatus bs = b.getBookingStatus();

        // Case 1: Booking COMPLETED + Payment APPROVED → đã hoàn tất lưu trú
        if (ps == PaymentStatus.APPROVED && bs == BookingStatus.COMPLETED) {
            return true;
        }

        // Case 2: Booking NO_SHOW + Payment DEPOSIT_FORFEITED → khách không đến
        if (ps == PaymentStatus.DEPOSIT_FORFEITED && bs == BookingStatus.NO_SHOW) {
            return true;
        }

        return false;
    }

    /**
     * Xác định ngày để xếp payment vào kỳ quyết toán tháng.
     * Ưu tiên: approvedAt → paidAt → booking.createdAt → now.
     */
    private LocalDate resolveSettlementDate(Payment p) {
        LocalDateTime approvedAt = p.getApprovedAt();
        LocalDateTime dt = approvedAt != null ? approvedAt :
                (p.getPaidAt() != null ? p.getPaidAt() :
                        (p.getBooking().getCreatedAt() != null ? p.getBooking().getCreatedAt() : LocalDateTime.now()));
        return dt.toLocalDate();
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    /**
     * Cơ sở tính commission = tiền thực thu qua hệ thống (payment.amount).
     * TravelMate chỉ tính commission trên tiền online, không tính 70% khách trả tại cơ sở.
     */
    private BigDecimal resolveCommissionBase(Payment p) {
        return p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
    }

    // ─── MARK PAID ────────────────────────────────────────────────────────────

    /**
     * Admin đánh dấu settlement đã thanh toán.
     *
     * @throws IllegalArgumentException nếu settlement không tồn tại hoặc đã PAID
     */
    @Transactional
    public PartnerSettlement markSettlementPaid(Long id, String note) {
        PartnerSettlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Settlement không tồn tại!"));

        if (settlement.getSettlementStatus() == SettlementStatus.PAID) {
            throw new IllegalArgumentException("Settlement này đã được thanh toán trước đó!");
        }

        settlement.setSettlementStatus(SettlementStatus.PAID);
        settlement.setSettlementDate(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            settlement.setNote(note);
        }

        return settlementRepository.save(settlement);
    }

    // ─── QUERY ────────────────────────────────────────────────────────────────

    /** Admin: xem tất cả settlement */
    public List<PartnerSettlement> getAllSettlementsForAdmin() {
        return settlementRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Partner: chỉ xem settlement của mình.
     * Security: lọc theo partner entity → không lộ data partner khác.
     */
    public List<PartnerSettlement> getSettlementsForPartner(User partner) {
        return settlementRepository.findByPartnerOrderByCreatedAtDesc(partner);
    }

    /** Đếm settlement PENDING — hiển thị badge admin */
    public long countPendingSettlements() {
        return settlementRepository.countBySettlementStatus(SettlementStatus.PENDING);
    }

    /**
     * Tìm settlement theo id — dùng trang chi tiết.
     */
    public PartnerSettlement findById(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Settlement #" + id + " không tồn tại!"));
    }

    /**
     * Trả về danh sách chi tiết từng booking trong kỳ của settlement.
     * Tái sử dụng logic isSettlementEligible + date-filter giống generateMonthlySettlements().
     * Admin và Partner đều dùng chung method này → đồng bộ dữ liệu tài chính.
     */
    public List<SettlementDetailItemDto> getBreakdownForSettlement(PartnerSettlement s) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Payment> partnerPayments =
                paymentRepository.findByBookingAccommodationOwnerAndPaymentStatusIn(
                        s.getPartner(), REVENUE_STATUSES);

        return partnerPayments.stream()
                .filter(this::isSettlementEligible)
                .filter(p -> {
                    LocalDate d = resolveSettlementDate(p);
                    return !d.isBefore(s.getPeriodStart()) && !d.isAfter(s.getPeriodEnd());
                })
                .map(p -> {
                    var b    = p.getBooking();
                    Room room = b.getRoom();

                    BigDecimal gross = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                    BigDecimal commBase = resolveCommissionBase(p);
                    BigDecimal commission = commissionService.calculateCommission(commBase, room);

                    BigDecimal effectiveRate = commissionService.getEffectiveCommissionRate(room);
                    int ratePercent = effectiveRate.multiply(BigDecimal.valueOf(100)).intValue();
                    boolean isOverride = commissionService.isRoomOverride(room);
                    String baseNote = (p.getPaymentOption() == PaymentOption.DEPOSIT_30)
                            ? ", CK trên cọc online" : ", CK trên 100% đã thu";
                    String rateDisplay = ratePercent + "%" + (isOverride ? " (theo phòng)" : " (mặc định)") + baseNote;

                    BigDecimal voucherDeduct = BigDecimal.ZERO;
                    if (b.getVoucherCostBearer() == VoucherCostBearer.PARTNER
                            && b.getDiscountAmount() != null) {
                        voucherDeduct = b.getDiscountAmount();
                    }

                    BigDecimal payout = gross.subtract(commission).subtract(voucherDeduct).max(BigDecimal.ZERO);

                    String statusVN = switch (p.getPaymentStatus().name()) {
                        case "APPROVED"          -> "Đã thanh toán";
                        case "DEPOSIT_FORFEITED" -> "Giữ cọc (no-show)";
                        default                  -> p.getPaymentStatus().name();
                    };

                    SettlementDetailItemDto dto = new SettlementDetailItemDto();
                    dto.setBookingCode(b.getBookingCode());
                    dto.setAccName(b.getAccommodation() != null ? b.getAccommodation().getName() : "—");
                    dto.setRoomName(room != null ? room.getRoomName() : "—");
                    dto.setCheckIn(b.getCheckIn() != null ? b.getCheckIn().format(fmt) : "—");
                    dto.setCheckOut(b.getCheckOut() != null ? b.getCheckOut().format(fmt) : "—");
                    dto.setGross(gross);
                    dto.setCommissionRateDisplay(rateDisplay);
                    dto.setCommissionAmount(commission);
                    dto.setVoucherCode(b.getVoucherCode());
                    dto.setVoucherDeductAmount(voucherDeduct);
                    dto.setPartnerPayout(payout);
                    dto.setPaymentStatusVN(statusVN);
                    return dto;
                })
                .toList();
    }
}