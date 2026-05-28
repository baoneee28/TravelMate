package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.RoomVoucherAssignment;
import com.travelmate.entity.User;
import com.travelmate.entity.Voucher;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.DiscountType;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.entity.enums.VoucherScope;
import com.travelmate.repository.RoomRepository;
import com.travelmate.repository.RoomVoucherAssignmentRepository;
import com.travelmate.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Kho voucher tập trung:
 * - Admin tạo và bật/tắt chương trình.
 * - USER_GLOBAL được dùng trực tiếp trên mọi phòng hợp lệ.
 * - PARTNER_ROOM là voucher trong kho để partner gắn vào phòng/căn của mình.
 * - costBearer quyết định việc trừ chi phí khỏi quyết toán partner.
 */
@SuppressWarnings("null")
@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final RoomRepository roomRepository;
    private final RoomVoucherAssignmentRepository assignmentRepository;

    public VoucherService(VoucherRepository voucherRepository,
                          RoomRepository roomRepository,
                          RoomVoucherAssignmentRepository assignmentRepository) {
        this.voucherRepository = voucherRepository;
        this.roomRepository = roomRepository;
        this.assignmentRepository = assignmentRepository;
    }

    private void validateVoucherInput(String code, String name,
                                      DiscountType discountType, BigDecimal discountValue,
                                      BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
                                      LocalDate startDate, LocalDate endDate) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã voucher không được để trống.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tên chương trình không được để trống.");
        }
        if (discountType == null) {
            throw new IllegalArgumentException("Vui lòng chọn loại giảm giá.");
        }
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0.");
        }
        if (discountType == DiscountType.PERCENT
                && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Giảm theo phần trăm không được vượt quá 100%.");
        }
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giá trị đơn tối thiểu không được âm.");
        }
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Giảm tối đa không được âm.");
        }
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu.");
        }
    }

    /**
     * Admin là tài khoản duy nhất được tạo voucher. Scope PARTNER_ROOM đưa
     * voucher vào kho để partner tự chọn phòng áp dụng.
     */
    public Voucher createAdminVoucher(String code, String name, String description,
                                      DiscountType discountType, BigDecimal discountValue,
                                      BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                      LocalDate startDate, LocalDate endDate,
                                      VoucherScope scope, VoucherCostBearer costBearer) {
        return createAdminVoucher(code, name, description, discountType, discountValue,
                maxDiscountAmount, minOrderAmount, startDate, endDate, scope, costBearer, null);
    }

    public Voucher createAdminVoucher(String code, String name, String description,
                                      DiscountType discountType, BigDecimal discountValue,
                                      BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                      LocalDate startDate, LocalDate endDate,
                                      VoucherScope scope, VoucherCostBearer costBearer,
                                      PropertyType propertyType) {
        validateVoucherInput(code, name, discountType, discountValue,
                minOrderAmount, maxDiscountAmount, startDate, endDate);

        String normalizedCode = code.trim().toUpperCase();
        if (voucherRepository.findByCode(normalizedCode).isPresent()) {
            throw new IllegalArgumentException("Mã voucher '" + normalizedCode + "' đã tồn tại.");
        }
        VoucherScope resolvedScope = scope != null ? scope : VoucherScope.USER_GLOBAL;
        if (resolvedScope == VoucherScope.PARTNER_ACCOMMODATION) {
            throw new IllegalArgumentException("Hãy dùng phạm vi kho Partner gắn vào phòng/căn để quản lý minh bạch.");
        }
        VoucherCostBearer resolvedBearer = resolvedScope == VoucherScope.USER_GLOBAL
                ? VoucherCostBearer.ADMIN
                : (costBearer != null ? costBearer : VoucherCostBearer.ADMIN);

        Voucher voucher = new Voucher();
        voucher.setCode(normalizedCode);
        voucher.setName(name.trim());
        voucher.setDescription(description);
        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(discountValue);
        voucher.setMaxDiscountAmount(discountType == DiscountType.PERCENT ? maxDiscountAmount : null);
        voucher.setMinOrderAmount(minOrderAmount != null ? minOrderAmount : BigDecimal.ZERO);
        voucher.setStartDate(startDate);
        voucher.setEndDate(endDate);
        voucher.setActive(true);
        voucher.setVoucherScope(resolvedScope);
        voucher.setPropertyType(resolvedScope == VoucherScope.PARTNER_ROOM ? propertyType : null);
        voucher.setCostBearer(resolvedBearer);
        voucher.setOwner(null);
        voucher.setAccommodation(null);
        voucher.setRoom(null);
        return voucherRepository.save(voucher);
    }

    /** Backward-compatible call for existing admin tests/code: creates a global platform voucher. */
    public Voucher createAdminVoucher(String code, String name, String description,
                                      DiscountType discountType, BigDecimal discountValue,
                                      BigDecimal maxDiscountAmount, BigDecimal minOrderAmount,
                                      LocalDate startDate, LocalDate endDate) {
        return createAdminVoucher(code, name, description, discountType, discountValue,
                maxDiscountAmount, minOrderAmount, startDate, endDate,
                VoucherScope.USER_GLOBAL, VoucherCostBearer.ADMIN);
    }

    /**
     * Partner chọn voucher trong kho và gắn vào phòng/căn đã được phê duyệt của mình.
     */
    @Transactional
    public RoomVoucherAssignment assignVoucherToRoom(User partner, Long voucherId, Long roomId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại."));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Phòng/căn không tồn tại."));
        validatePartnerCanManageRoom(partner, room);
        if (voucher.getVoucherScope() != VoucherScope.PARTNER_ROOM) {
            throw new IllegalArgumentException("Voucher toàn hệ thống được áp dụng tự động, không cần gắn vào phòng.");
        }
        if (!voucher.isCurrentlyValid()) {
            throw new IllegalArgumentException("Voucher đã hết hạn hoặc đang tắt, không thể gắn.");
        }
        validateVoucherMatchesPartnerPropertyType(voucher, partner);

        RoomVoucherAssignment assignment = assignmentRepository.findByVoucherAndRoom(voucher, room)
                .orElseGet(RoomVoucherAssignment::new);
        assignment.setVoucher(voucher);
        assignment.setRoom(room);
        assignment.setAssignedByPartner(partner);
        assignment.setActive(true);
        return assignmentRepository.save(assignment);
    }

    @Transactional
    public void removeAssignment(User partner, Long assignmentId) {
        RoomVoucherAssignment assignment = assignmentRepository
                .findByIdAndAssignedByPartner(assignmentId, partner)
                .orElseThrow(() -> new SecurityException("Bạn không có quyền thay đổi liên kết voucher này."));
        validatePartnerCanManageRoom(partner, assignment.getRoom());
        assignment.setActive(false);
        assignmentRepository.save(assignment);
    }

    public List<Voucher> getPartnerRoomCatalog(User partner) {
        if (partner == null || partner.getPartnerPropertyType() == null) {
            return List.of();
        }
        return voucherRepository.findByVoucherScopeOrderByCreatedAtDesc(VoucherScope.PARTNER_ROOM)
                .stream()
                .filter(Voucher::isCurrentlyValid)
                .filter(v -> v.getPropertyType() == null
                        || v.getPropertyType() == partner.getPartnerPropertyType())
                .toList();
    }

    public List<RoomVoucherAssignment> getAssignmentsForPartner(User partner) {
        return assignmentRepository.findByAssignedByPartnerOrderByAssignedAtDesc(partner).stream()
                .filter(a -> isManagedByPartner(a.getRoom().getAccommodation(), partner))
                .toList();
    }

    public Map<Long, List<RoomVoucherAssignment>> getActiveAssignmentsForRooms(List<Room> rooms) {
        Map<Long, List<RoomVoucherAssignment>> result = new LinkedHashMap<>();
        if (rooms != null) {
            for (Room room : rooms) {
                result.put(room.getId(), assignmentRepository.findByRoomAndActiveTrue(room));
            }
        }
        return result;
    }

    /**
     * Danh sách voucher nên hiện trên trang đặt phòng: voucher chung và voucher
     * partner đã gắn vào đúng phòng.
     */
    public List<Voucher> getApplicableVouchers(Room room) {
        Map<Long, Voucher> distinct = new LinkedHashMap<>();
        getPublicVouchers().forEach(v -> distinct.put(v.getId(), v));
        assignmentRepository.findByRoomAndActiveTrue(room).stream()
                .map(RoomVoucherAssignment::getVoucher)
                .filter(Voucher::isCurrentlyValid)
                .filter(v -> v.getVoucherScope() == VoucherScope.PARTNER_ROOM)
                .filter(v -> matchesRoomPropertyType(v, room))
                .forEach(v -> distinct.put(v.getId(), v));
        return List.copyOf(distinct.values());
    }

    /**
     * Validate code trên server; PARTNER_ROOM chỉ hợp lệ sau khi partner đã
     * gắn vào đúng phòng.
     */
    public Voucher validateVoucher(String code, BigDecimal orderAmount, Room room) {
        if (code == null || code.isBlank()) {
            return null;
        }
        Voucher voucher = voucherRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher '" + code + "' không tồn tại."));
        if (!voucher.isCurrentlyValid()) {
            throw new IllegalArgumentException("Voucher đã hết hạn hoặc không còn hiệu lực.");
        }
        BigDecimal minimum = voucher.getMinOrderAmount() != null
                ? voucher.getMinOrderAmount() : BigDecimal.ZERO;
        if (orderAmount == null || orderAmount.compareTo(minimum) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt tối thiểu " + formatVnd(minimum) + ".");
        }
        if (voucher.getVoucherScope() == VoucherScope.PARTNER_ROOM
                && !assignmentRepository.existsByVoucherAndRoomAndActiveTrue(voucher, room)) {
            throw new IllegalArgumentException("Voucher này chưa được đối tác áp dụng cho phòng/căn đang chọn.");
        }
        if (voucher.getVoucherScope() == VoucherScope.PARTNER_ROOM
                && !matchesRoomPropertyType(voucher, room)) {
            throw new IllegalArgumentException("Voucher này không áp dụng cho loại lưu trú của phòng/căn đã chọn.");
        }
        if (voucher.getVoucherScope() == VoucherScope.PARTNER_ACCOMMODATION) {
            Long voucherAccommodationId = voucher.getAccommodation() != null
                    ? voucher.getAccommodation().getId() : null;
            Long roomAccommodationId = room != null && room.getAccommodation() != null
                    ? room.getAccommodation().getId() : null;
            if (voucher.getOwner() != null || voucherAccommodationId == null
                    || !voucherAccommodationId.equals(roomAccommodationId)) {
                throw new IllegalArgumentException("Voucher theo cơ sở cũ không còn được áp dụng. Vui lòng dùng kho voucher mới.");
            }
        }
        return voucher;
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderAmount) {
        if (voucher == null || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = orderAmount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null
                    && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else {
            discount = voucher.getDiscountValue().setScale(0, RoundingMode.HALF_UP);
        }
        discount = discount.min(orderAmount);
        BigDecimal minVnpay = new BigDecimal("5000");
        if (orderAmount.subtract(discount).compareTo(minVnpay) < 0) {
            discount = orderAmount.subtract(minVnpay).max(BigDecimal.ZERO);
        }
        return discount;
    }

    public List<Voucher> getPublicVouchers() {
        return voucherRepository.findByVoucherScopeOrderByCreatedAtDesc(VoucherScope.USER_GLOBAL)
                .stream().filter(Voucher::isCurrentlyValid).toList();
    }

    public List<Voucher> getAllVouchersForAdmin() {
        return voucherRepository.findAllByOrderByCreatedAtDesc();
    }

    public Voucher toggleActive(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại."));
        voucher.setActive(!Boolean.TRUE.equals(voucher.getActive()));
        return voucherRepository.save(voucher);
    }

    public Optional<Voucher> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return voucherRepository.findByCode(code.trim().toUpperCase());
    }

    private void validatePartnerCanManageRoom(User partner, Room room) {
        if (room.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("Chỉ được gắn voucher vào phòng/căn đã được Admin duyệt.");
        }
        validatePartnerCanManageAccommodation(partner, room.getAccommodation());
    }

    private void validatePartnerCanManageAccommodation(User partner, Accommodation accommodation) {
        if (partner == null || partner.getId() == null || accommodation == null
                || accommodation.getOwner() == null
                || !partner.getId().equals(accommodation.getOwner().getId())) {
            throw new SecurityException("Bạn không có quyền quản lý phòng/căn này.");
        }
        if (partner.getPartnerPropertyType() == null
                || partner.getPartnerPropertyType() != accommodation.getPropertyType()) {
            throw new SecurityException("Phòng/căn không thuộc loại lưu trú đã đăng ký của đối tác.");
        }
    }

    private void validateVoucherMatchesPartnerPropertyType(Voucher voucher, User partner) {
        if (partner == null || partner.getPartnerPropertyType() == null
                || (voucher.getPropertyType() != null
                    && voucher.getPropertyType() != partner.getPartnerPropertyType())) {
            throw new SecurityException("Voucher không thuộc loại lưu trú đã đăng ký của đối tác.");
        }
    }

    private boolean matchesRoomPropertyType(Voucher voucher, Room room) {
        return room != null
                && room.getAccommodation() != null
                && (voucher.getPropertyType() == null
                    || voucher.getPropertyType() == room.getAccommodation().getPropertyType());
    }

    private boolean isManagedByPartner(Accommodation accommodation, User partner) {
        return accommodation != null && partner != null
                && accommodation.getOwner() != null
                && partner.getId() != null
                && partner.getId().equals(accommodation.getOwner().getId())
                && partner.getPartnerPropertyType() == accommodation.getPropertyType();
    }

    private String formatVnd(BigDecimal amount) {
        return String.format("%,.0fđ", amount != null ? amount : BigDecimal.ZERO);
    }
}
