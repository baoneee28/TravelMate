package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.Voucher;
import com.travelmate.entity.enums.DiscountType;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.entity.enums.VoucherScope;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.RoomRepository;
import com.travelmate.repository.VoucherRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * VoucherService — Quản lý tạo, validate, và tính toán giảm giá voucher.
 *
 * Quy tắc:
 *  - Admin tạo: USER_GLOBAL, costBearer=ADMIN → không trừ settlement partner
 *  - Partner tạo: PARTNER_ACCOMMODATION/ROOM, costBearer=PARTNER
 *    → chỉ tạo được voucher cho accommodation/room mà partner đó sở hữu
 *    → trừ vào settlement partner
 */
@SuppressWarnings("null")
@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;

    public VoucherService(VoucherRepository voucherRepository,
                          AccommodationRepository accommodationRepository,
                          RoomRepository roomRepository) {
        this.voucherRepository = voucherRepository;
        this.accommodationRepository = accommodationRepository;
        this.roomRepository = roomRepository;
    }

    // ─── SHARED VALIDATION ────────────────────────────────────────────────────

    /**
     * Validate tất cả input voucher — dùng chung cho Admin và Partner.
     * Đảm bảo server-side validation chặt chẽ, không phụ thuộc vào JS frontend.
     *
     * @param code             Mã voucher (không rỗng, check trùng sau khi trim+uppercase)
     * @param name             Tên voucher (không rỗng)
     * @param discountType     Loại giảm (PERCENT hoặc FIXED_AMOUNT)
     * @param discountValue    Giá trị giảm (> 0; nếu PERCENT thì ≤ 100)
     * @param minOrderAmount   Đơn tối thiểu (không âm)
     * @param maxDiscountAmount Giảm tối đa (không âm)
     * @param startDate        Ngày bắt đầu (không null)
     * @param endDate          Ngày kết thúc (không null, >= startDate)
     * @throws IllegalArgumentException nếu bất kỳ rule nào vi phạm
     */
    private void validateVoucherInput(String code, String name,
                                      DiscountType discountType, BigDecimal discountValue,
                                      BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
                                      LocalDate startDate, LocalDate endDate) {
        // ── Mã voucher ──
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("Mã voucher không được để trống!");

        // ── Tên voucher ──
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Tên voucher không được để trống!");

        // ── Giá trị giảm ──
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0!");

        // ── % không vượt 100 ──
        if (discountType == DiscountType.PERCENT
                && discountValue.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new IllegalArgumentException("Giảm theo % không được vượt quá 100%!");

        // ── Đơn tối thiểu ──
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Giá trị đơn tối thiểu không được âm!");

        // ── Giảm tối đa ──
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Giảm tối đa (cap) không được âm!");

        // ── Ngày không trống ──
        if (startDate == null || endDate == null)
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không được để trống!");

        // ── endDate >= startDate ──
        if (endDate.isBefore(startDate))
            throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu!");
    }

    // ─── ADMIN OPERATIONS ─────────────────────────────────────────────────────

    /**
     * Admin tạo voucher USER_GLOBAL (áp dụng cho mọi phòng, admin chịu chi phí).
     */
    public Voucher createAdminVoucher(String code, String name, String description,
                                      DiscountType discountType, BigDecimal discountValue,
                                      BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                      LocalDate startDate, LocalDate endDate) {
        // ── Validate đầy đủ server-side ──
        validateVoucherInput(code, name, discountType, discountValue,
                             minOrderAmount, maxDiscountAmount, startDate, endDate);

        // ── Check trùng mã (sau normalize) ──
        if (voucherRepository.findByCode(code.trim().toUpperCase()).isPresent())
            throw new IllegalArgumentException("Mã voucher '" + code.trim().toUpperCase() + "' đã tồn tại!");

        Voucher voucher = new Voucher();
        voucher.setCode(code.trim().toUpperCase());
        voucher.setName(name.trim());
        voucher.setDescription(description);
        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(discountValue);
        voucher.setMaxDiscountAmount(maxDiscountAmount);
        voucher.setMinOrderAmount(minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO);
        voucher.setStartDate(startDate);
        voucher.setEndDate(endDate);
        voucher.setActive(true);
        voucher.setVoucherScope(VoucherScope.USER_GLOBAL);
        voucher.setCostBearer(VoucherCostBearer.ADMIN);
        voucher.setOwner(null);

        return voucherRepository.save(voucher);
    }

    /**
     * Partner tạo voucher cho accommodation của mình (PARTNER_ACCOMMODATION).
     *
     * @throws SecurityException nếu accommodationId không thuộc partner
     */
    public Voucher createPartnerVoucherForAccommodation(User partner, Long accommodationId,
                                                         String code, String name, String description,
                                                         DiscountType discountType, BigDecimal discountValue,
                                                         BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                                         LocalDate startDate, LocalDate endDate) {
        Accommodation acc = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new IllegalArgumentException("Accommodation không tồn tại!"));

        // Security: chỉ partner sở hữu mới được tạo
        if (acc.getOwner() == null || !acc.getOwner().getId().equals(partner.getId())) {
            throw new SecurityException("Bạn không có quyền tạo voucher cho accommodation này!");
        }

        // ── Validate đầy đủ server-side (giống Admin) ──
        validateVoucherInput(code, name, discountType, discountValue,
                             minOrderAmount, maxDiscountAmount, startDate, endDate);

        // ── Check trùng mã (normalize trước khi check) ──
        if (voucherRepository.findByCode(code.trim().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("Mã voucher '" + code.trim().toUpperCase() + "' đã tồn tại!");
        }

        Voucher voucher = buildPartnerVoucher(code, name, description, discountType, discountValue,
                maxDiscountAmount, minOrderAmount, startDate, endDate, partner);
        voucher.setVoucherScope(VoucherScope.PARTNER_ACCOMMODATION);
        voucher.setAccommodation(acc);

        return voucherRepository.save(voucher);
    }

    /**
     * Partner tạo voucher cho room cụ thể của mình (PARTNER_ROOM).
     *
     * @throws SecurityException nếu room không thuộc partner
     */
    public Voucher createPartnerVoucherForRoom(User partner, Long roomId,
                                                String code, String name, String description,
                                                DiscountType discountType, BigDecimal discountValue,
                                                BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                                LocalDate startDate, LocalDate endDate) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Phòng không tồn tại!"));

        // Security: chỉ partner sở hữu accommodation chứa room mới được tạo
        if (room.getAccommodation().getOwner() == null
                || !room.getAccommodation().getOwner().getId().equals(partner.getId())) {
            throw new SecurityException("Bạn không có quyền tạo voucher cho phòng này!");
        }

        // ── Validate đầy đủ server-side (giống Admin) ──
        validateVoucherInput(code, name, discountType, discountValue,
                             minOrderAmount, maxDiscountAmount, startDate, endDate);

        // ── Check trùng mã (normalize trước khi check) ──
        if (voucherRepository.findByCode(code.trim().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("Mã voucher '" + code.trim().toUpperCase() + "' đã tồn tại!");
        }

        Voucher voucher = buildPartnerVoucher(code, name, description, discountType, discountValue,
                maxDiscountAmount, minOrderAmount, startDate, endDate, partner);
        voucher.setVoucherScope(VoucherScope.PARTNER_ROOM);
        voucher.setRoom(room);

        return voucherRepository.save(voucher);
    }

    // ─── VALIDATE & CALCULATE ─────────────────────────────────────────────────

    /**
     * Validate voucher code và kiểm tra điều kiện áp dụng.
     * Trả về voucher nếu hợp lệ, throw exception nếu không.
     *
     * @param code         Mã voucher user nhập
     * @param orderAmount  Giá trị đơn hàng (totalBeforeDiscount)
     * @param room         Phòng đang đặt (để kiểm tra scope PARTNER_ROOM)
     */
    public Voucher validateVoucher(String code, BigDecimal orderAmount, Room room) {
        if (code == null || code.isBlank()) return null;

        Voucher voucher = voucherRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher '" + code + "' không tồn tại!"));

        if (!voucher.isCurrentlyValid()) {
            throw new IllegalArgumentException("Voucher đã hết hạn hoặc không còn hiệu lực!");
        }

        if (orderAmount.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu "
                    + formatVnd(voucher.getMinOrderAmount()) + " để áp dụng voucher này!");
        }

        // Kiểm tra scope PARTNER_ROOM: phòng phải đúng
        if (voucher.getVoucherScope() == VoucherScope.PARTNER_ROOM) {
            if (voucher.getRoom() == null || !voucher.getRoom().getId().equals(room.getId())) {
                throw new IllegalArgumentException("Voucher này chỉ áp dụng cho phòng cụ thể, không áp dụng cho phòng đang đặt!");
            }
        }

        // Kiểm tra scope PARTNER_ACCOMMODATION: accommodation phải đúng
        if (voucher.getVoucherScope() == VoucherScope.PARTNER_ACCOMMODATION) {
            Long vAcc = voucher.getAccommodation() != null ? voucher.getAccommodation().getId() : null;
            Long rAcc = room.getAccommodation() != null ? room.getAccommodation().getId() : null;
            if (vAcc == null || !vAcc.equals(rAcc)) {
                throw new IllegalArgumentException("Voucher này chỉ áp dụng cho lưu trú " + (voucher.getAccommodation() != null ? voucher.getAccommodation().getName() : "") + "!");
            }
        }

        return voucher;
    }

    /**
     * Tính số tiền giảm giá từ voucher và giá trị đơn hàng.
     *
     * Guard: Không cho giảm giá làm tổng tiền về dưới 5.000₫ (VNPAY minimum).
     * Nếu voucher giảm quá nhiều → giới hạn discount = orderAmount - 5.000₫.
     *
     * @param voucher     Voucher đã validate
     * @param orderAmount Giá trị đơn hàng
     * @return            Số tiền được giảm (≥ 0)
     */
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount) {
        if (voucher == null || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = orderAmount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            // Áp cap nếu có maxDiscountAmount
            if (voucher.getMaxDiscountAmount() != null
                    && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else {
            // FIXED_AMOUNT
            discount = voucher.getDiscountValue().setScale(0, RoundingMode.HALF_UP);
        }

        // Không giảm nhiều hơn giá trị đơn hàng
        if (discount.compareTo(orderAmount) > 0) discount = orderAmount;

        // Guard: Không cho giảm giá làm tổng tiền về dưới 5.000₫ (VNPAY minimum)
        // Nếu sau giảm giá mà tổng tiền < 5000₫ → giới hạn discount
        BigDecimal MIN_VNPAY = new BigDecimal("5000");
        BigDecimal afterDiscount = orderAmount.subtract(discount);
        if (afterDiscount.compareTo(MIN_VNPAY) < 0) {
            // Giảm tối đa sao cho còn lại ít nhất 5.000₫
            discount = orderAmount.subtract(MIN_VNPAY);
            if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;
        }

        return discount;
    }

    // ─── QUERY ────────────────────────────────────────────────────────────────

    /** Trang /vouchers công khai: chỉ USER_GLOBAL còn hiệu lực */
    public List<Voucher> getPublicVouchers() {
        return voucherRepository.findByVoucherScopeOrderByCreatedAtDesc(VoucherScope.USER_GLOBAL)
                .stream()
                .filter(Voucher::isCurrentlyValid)
                .collect(Collectors.toList());
    }

    /** Admin xem tất cả voucher */
    public List<Voucher> getAllVouchersForAdmin() {
        return voucherRepository.findAllByOrderByCreatedAtDesc();
    }

    /** Partner xem voucher của mình */
    public List<Voucher> getVouchersForPartner(User partner) {
        return voucherRepository.findByOwnerOrderByCreatedAtDesc(partner);
    }

    /** Toggle active/inactive */
    public Voucher toggleActive(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại!"));
        voucher.setActive(!voucher.getActive());
        return voucherRepository.save(voucher);
    }

    /** Tìm voucher theo code (không throw, trả Optional) */
    public Optional<Voucher> findByCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return voucherRepository.findByCode(code.trim().toUpperCase());
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private Voucher buildPartnerVoucher(String code, String name, String description,
                                         DiscountType discountType, BigDecimal discountValue,
                                         BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                         LocalDate startDate, LocalDate endDate, User partner) {
        Voucher v = new Voucher();
        v.setCode(code.trim().toUpperCase());
        v.setName(name);
        v.setDescription(description);
        v.setDiscountType(discountType);
        v.setDiscountValue(discountValue);
        v.setMaxDiscountAmount(maxDiscountAmount);
        v.setMinOrderAmount(minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO);
        v.setStartDate(startDate);
        v.setEndDate(endDate);
        v.setActive(true);
        v.setCostBearer(VoucherCostBearer.PARTNER);
        v.setOwner(partner);
        return v;
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,.0fđ", amount);
    }
}
