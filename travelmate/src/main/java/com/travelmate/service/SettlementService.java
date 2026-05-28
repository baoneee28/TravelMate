package com.travelmate.service;

import com.travelmate.dto.SettlementDetailItemDto;
import com.travelmate.entity.Accommodation;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 *  - Booking ONLINE + Payment DEPOSIT_FORFEITED + BookingStatus NO_SHOW/CANCELLED
 *  → Chỉ booking đã hoàn tất lưu trú hoặc khách không đến mới được quyết toán.
 *
 * Quy tắc commission dung rate snapshot booking:
 *   - Moi don ONLINE: commBase = khoan TravelMate da thu online
 *   - DEPOSIT_30: chi tinh tren coc online 30%, ke ca hoan tat hoac mat coc
 *   - FULL_PAYMENT: tinh tren khoan thanh toan online 100%
 *   payout = max(0, gross - commission - voucherPartnerDeduct)
 */
@SuppressWarnings("null")
@Service
public class SettlementService {

    private final PartnerSettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CommissionService commissionService;
    private final PartnerWalletService partnerWalletService;

    @Value("${travelmate.demo-mode:true}")
    private boolean demoMode;

    private static final List<PaymentStatus> REVENUE_STATUSES =
            List.of(PaymentStatus.APPROVED, PaymentStatus.DEPOSIT_FORFEITED);

    public SettlementService(PartnerSettlementRepository settlementRepository,
                             PaymentRepository paymentRepository,
                             UserRepository userRepository,
                             CommissionService commissionService,
                             PartnerWalletService partnerWalletService) {
        this.settlementRepository = settlementRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.commissionService = commissionService;
        this.partnerWalletService = partnerWalletService;
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
        YearMonth currentMonth = YearMonth.now();
        LocalDate periodStart = lastMonth.atDay(1);
        LocalDate periodEnd   = lastMonth.atEndOfMonth();
        LocalDate scheduledPayoutDate = currentMonth.atDay(10);

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
                    .filter(p -> isManagedByPartner(p, partner))
                    .toList();

            if (partnerPayments.isEmpty()) continue; // Không có doanh thu → không tạo

            // Tính toán
            BigDecimal gross = BigDecimal.ZERO;
            BigDecimal commission = BigDecimal.ZERO;
            BigDecimal voucherDeduct = BigDecimal.ZERO;
            BigDecimal payout = BigDecimal.ZERO;

            for (Payment p : partnerPayments) {
                BigDecimal g = resolveOnlinePaid(p);
                BigDecimal c = resolveCommissionAmount(p);
                BigDecimal vd = resolvePartnerVoucherAmount(p);
                BigDecimal partnerPayout = resolvePartnerPayout(p);

                gross = gross.add(g);
                commission = commission.add(c);
                voucherDeduct = voucherDeduct.add(vd);
                payout = payout.add(partnerPayout);
            }

            PartnerSettlement settlement = new PartnerSettlement();
            settlement.setPartner(partner);
            settlement.setPeriodStart(periodStart);
            settlement.setPeriodEnd(periodEnd);
            settlement.setGrossAmount(gross);
            settlement.setCommissionAmount(commission);
            settlement.setVoucherDeductionAmount(voucherDeduct);
            settlement.setPayoutAmount(payout);
            settlement.setScheduledPayoutDate(scheduledPayoutDate);
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
     * - ONLINE + DEPOSIT_FORFEITED + NO_SHOW/CANCELLED → coc bi giu
     *
     * Loại bỏ: PENDING_ADMIN_APPROVAL, CONFIRMED, CHECKED_IN, CANCELLED không giữ cọc,
     *           REFUNDED, DIRECT, MANUAL_BLOCK...
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

        // Case 2: cọc bị giữ do khách hủy hoặc không đến
        if (ps == PaymentStatus.DEPOSIT_FORFEITED
                && (bs == BookingStatus.NO_SHOW || bs == BookingStatus.CANCELLED)) {
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

    private BigDecimal resolveCommissionBase(Payment p) {
        return resolveOnlinePaid(p);
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
                .subtract(resolvePartnerVoucherAmount(p)).max(BigDecimal.ZERO);
    }

    private BigDecimal resolveOnlinePaid(Payment p) {
        return p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
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

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    // ─── MARK PAID ────────────────────────────────────────────────────────────

    /**
     * Admin đánh dấu settlement đã thanh toán.
     *
     * @throws IllegalArgumentException nếu settlement không tồn tại hoặc đã PAID
     */
    @Transactional
    public PartnerSettlement markSettlementPaid(Long id, String note) {
        return markSettlementPaid(id, note, null);
    }

    @Transactional
    public PartnerSettlement markSettlementPaid(Long id, String note, User admin) {
        PartnerSettlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Settlement không tồn tại!"));

        if (settlement.getSettlementStatus() == SettlementStatus.PAID) {
            throw new IllegalArgumentException("Settlement này đã được thanh toán trước đó!");
        }
        if (settlement.getSettlementStatus() == SettlementStatus.CANCELLED) {
            throw new IllegalArgumentException("Settlement đã bị hủy, không thể thanh toán!");
        }

        LocalDate scheduledDate = settlement.getScheduledPayoutDate();
        if (scheduledDate != null && LocalDate.now().isBefore(scheduledDate) && !demoMode) {
            throw new IllegalArgumentException("Chưa đến ngày chi trả dự kiến "
                    + scheduledDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + ". Có thể bật travelmate.demo-mode=true để demo trước ngày chi trả.");
        }

        // Settlement legacy phai duoc doi soat theo Huong A truoc khi payout duoc cong vao vi.
        refreshPendingSettlementTotals(List.of(settlement));

        settlement.setSettlementStatus(SettlementStatus.PAID);
        settlement.setSettlementDate(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            settlement.setNote(note);
        }

        PartnerSettlement saved = settlementRepository.save(settlement);
        partnerWalletService.creditSettlement(saved, admin);
        return saved;
    }

    // ─── QUERY ────────────────────────────────────────────────────────────────

    /**
     * Admin: xem tất cả settlement.
     * Settlement dang cho chi tra duoc dong bo lai tu breakdown theo quy tac
     * hien tai; settlement da PAID duoc giu nguyen de bao toan lich su vi.
     */
    @Transactional
    public List<PartnerSettlement> getAllSettlementsForAdmin() {
        List<PartnerSettlement> settlements = settlementRepository.findAllByOrderByCreatedAtDesc();
        refreshPendingSettlementTotals(settlements);
        return settlements;
    }

    /**
     * Partner: chỉ xem settlement của mình.
     * Security: lọc theo partner entity → không lộ data partner khác.
     */
    @Transactional
    public List<PartnerSettlement> getSettlementsForPartner(User partner) {
        List<PartnerSettlement> settlements = settlementRepository.findByPartnerOrderByCreatedAtDesc(partner);
        refreshPendingSettlementTotals(settlements);
        return settlements;
    }

    /**
     * Tổng trên trang chi tiết được tính lại từ các booking đang hiển thị.
     * Cách này giúp footer luôn khớp từng dòng, kể cả settlement cũ còn lưu thiếu voucher.
     */
    public Map<String, BigDecimal> calculateBreakdownTotals(List<SettlementDetailItemDto> breakdown) {
        List<SettlementDetailItemDto> items = breakdown != null ? breakdown : List.of();
        BigDecimal totalOrder = BigDecimal.ZERO;
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal onsite = BigDecimal.ZERO;
        BigDecimal commission = BigDecimal.ZERO;
        BigDecimal voucher = BigDecimal.ZERO;
        BigDecimal payout = BigDecimal.ZERO;

        for (SettlementDetailItemDto item : items) {
            totalOrder = totalOrder.add(safeAmount(item.getTotalOrderAmount()));
            gross = gross.add(safeAmount(item.getGross()));
            onsite = onsite.add(safeAmount(item.getOnsiteAmount()));
            commission = commission.add(safeAmount(item.getCommissionAmount()));
            voucher = voucher.add(safeAmount(item.getVoucherDeductAmount()));
            payout = payout.add(safeAmount(item.getPartnerPayout()));
        }

        return Map.of(
                "totalOrder", totalOrder,
                "gross", gross,
                "onsite", onsite,
                "commission", commission,
                "voucher", voucher,
                "payout", payout
        );
    }

    private void refreshPendingSettlementTotals(List<PartnerSettlement> settlements) {
        for (PartnerSettlement settlement : settlements != null ? settlements : List.<PartnerSettlement>of()) {
            if (settlement.getSettlementStatus() != SettlementStatus.PENDING) {
                continue;
            }
            List<SettlementDetailItemDto> breakdown = getBreakdownForSettlement(settlement);
            if (breakdown.isEmpty()) {
                continue;
            }
            Map<String, BigDecimal> totals = calculateBreakdownTotals(breakdown);
            BigDecimal gross = totals.get("gross");
            BigDecimal commission = totals.get("commission");
            BigDecimal voucher = totals.get("voucher");
            BigDecimal payout = totals.get("payout");
            if (amountChanged(settlement.getGrossAmount(), gross)
                    || amountChanged(settlement.getCommissionAmount(), commission)
                    || amountChanged(settlement.getVoucherDeductionAmount(), voucher)
                    || amountChanged(settlement.getPayoutAmount(), payout)) {
                settlement.setGrossAmount(gross);
                settlement.setCommissionAmount(commission);
                settlement.setVoucherDeductionAmount(voucher);
                settlement.setPayoutAmount(payout);
                settlementRepository.save(settlement);
            }
        }
    }

    private boolean amountChanged(BigDecimal stored, BigDecimal recalculated) {
        return safeAmount(stored).compareTo(safeAmount(recalculated)) != 0;
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
                .filter(p -> isManagedByPartner(p, s.getPartner()))
                .filter(this::isSettlementEligible)
                .filter(p -> {
                    LocalDate d = resolveSettlementDate(p);
                    return !d.isBefore(s.getPeriodStart()) && !d.isAfter(s.getPeriodEnd());
                })
                .map(p -> {
                    var b    = p.getBooking();
                    Room room = b.getRoom();

                    BigDecimal gross = resolveOnlinePaid(p);
                    BigDecimal commBase = resolveCommissionBase(p);
                    BigDecimal commission = resolveCommissionAmount(p);

                    BigDecimal effectiveRate = hasFinancialSnapshot(p)
                            ? b.getCommissionRateSnapshot() : commissionService.getEffectiveCommissionRate(room);
                    int ratePercent = effectiveRate.multiply(BigDecimal.valueOf(100)).intValue();
                    boolean isOverride = hasFinancialSnapshot(p)
                            ? "ROOM_OVERRIDE".equals(b.getCommissionSourceSnapshot())
                            : commissionService.isRoomOverride(room);
                    String baseNote;
                    if (p.getPaymentOption() == PaymentOption.DEPOSIT_30
                            && p.getPaymentStatus() == PaymentStatus.DEPOSIT_FORFEITED) {
                        baseNote = ", HH trên cọc online bị giữ";
                    } else if (p.getPaymentOption() == PaymentOption.DEPOSIT_30) {
                        baseNote = ", HH trên cọc online 30%";
                    } else {
                        baseNote = ", HH trên tiền online 100%";
                    }
                    String rateDisplay = ratePercent + "%" + (isOverride ? " (theo phòng)" : " (mặc định)") + baseNote;

                    BigDecimal voucherDeduct = resolvePartnerVoucherAmount(p);
                    BigDecimal payout = resolvePartnerPayout(p);

                    String statusVN = switch (p.getPaymentStatus().name()) {
                        case "APPROVED"          -> "Đã thanh toán";
                        case "DEPOSIT_FORFEITED" -> "Giữ cọc (hủy/no-show)";
                        default                  -> p.getPaymentStatus().name();
                    };

                    SettlementDetailItemDto dto = new SettlementDetailItemDto();
                    dto.setBookingCode(b.getBookingCode());
                    dto.setAccName(b.getAccommodation() != null ? b.getAccommodation().getName() : "—");
                    dto.setRoomName(room != null ? room.getRoomName() : "—");
                    dto.setCheckIn(b.getCheckIn() != null ? b.getCheckIn().format(fmt) : "—");
                    dto.setCheckOut(b.getCheckOut() != null ? b.getCheckOut().format(fmt) : "—");
                    dto.setTotalOrderAmount(b.getTotalAmount());
                    dto.setGross(gross);
                    dto.setOnsiteAmount(resolveOnsiteAmount(p));
                    dto.setCommissionBaseLabel(baseNote.substring(2));
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

    private boolean isManagedByPartner(Payment payment, User partner) {
        if (payment == null || payment.getBooking() == null) {
            return false;
        }
        return isManagedByPartner(payment.getBooking().getAccommodation(), partner);
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
}
