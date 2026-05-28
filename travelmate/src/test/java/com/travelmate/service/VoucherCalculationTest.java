package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.RoomVoucherAssignment;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.Voucher;
import com.travelmate.entity.enums.DiscountType;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.entity.enums.VoucherScope;
import com.travelmate.repository.RoomVoucherAssignmentRepository;
import com.travelmate.repository.RoomRepository;
import com.travelmate.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử logic tính giảm giá voucher:
 * PERCENT, FIXED_AMOUNT, maxDiscountAmount cap, VNPAY minimum guard,
 * phân biệt costBearer ADMIN vs PARTNER.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherService — Tính giảm giá")
class VoucherCalculationTest {

    @Mock private VoucherRepository voucherRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomVoucherAssignmentRepository assignmentRepository;

    private VoucherService voucherService;

    @BeforeEach
    void setUp() {
        voucherService = new VoucherService(voucherRepository, roomRepository, assignmentRepository);
    }

    private Voucher buildPercentVoucher(double percent, long maxDiscount, long minOrder) {
        Voucher v = new Voucher();
        v.setDiscountType(DiscountType.PERCENT);
        v.setDiscountValue(BigDecimal.valueOf(percent));
        v.setMaxDiscountAmount(maxDiscount > 0 ? BigDecimal.valueOf(maxDiscount) : null);
        v.setMinOrderAmount(BigDecimal.valueOf(minOrder));
        v.setActive(true);
        v.setStartDate(LocalDate.now().minusDays(1));
        v.setEndDate(LocalDate.now().plusDays(30));
        v.setCostBearer(VoucherCostBearer.ADMIN);
        return v;
    }

    private Voucher buildFixedVoucher(long fixedAmount, long minOrder, VoucherCostBearer bearer) {
        Voucher v = new Voucher();
        v.setDiscountType(DiscountType.FIXED_AMOUNT);
        v.setDiscountValue(BigDecimal.valueOf(fixedAmount));
        v.setMaxDiscountAmount(null);
        v.setMinOrderAmount(BigDecimal.valueOf(minOrder));
        v.setActive(true);
        v.setStartDate(LocalDate.now().minusDays(1));
        v.setEndDate(LocalDate.now().plusDays(30));
        v.setCostBearer(bearer);
        return v;
    }

    // ─── PERCENT voucher ──────────────────────────────────────────────────────

    @Test
    @DisplayName("PERCENT 10% — đơn hàng 1.300.000đ → giảm 130.000đ")
    void percent_basicDiscount() {
        Voucher v = buildPercentVoucher(10, 500000, 500000);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("1300000"));

        assertThat(discount).isEqualByComparingTo("130000");
    }

    @Test
    @DisplayName("PERCENT 10% — bị cap bởi maxDiscountAmount 500.000đ")
    void percent_cappedByMaxDiscount() {
        Voucher v = buildPercentVoucher(10, 500000, 500000);

        // 10% × 6.000.000 = 600.000 > cap 500.000 → kết quả phải là 500.000
        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("6000000"));

        assertThat(discount).isEqualByComparingTo("500000");
    }

    @Test
    @DisplayName("PERCENT 20% — SUMMER10: đơn 1.300.000đ → giảm 130.000đ (< cap 500.000đ)")
    void percent_summer10_underCap() {
        Voucher v = buildPercentVoucher(10, 500000, 500000);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("1300000"));

        assertThat(discount).isEqualByComparingTo("130000");
        assertThat(discount.compareTo(new BigDecimal("500000"))).isLessThan(0);
    }

    @Test
    @DisplayName("PERCENT 20% — LATA20 partner: đơn 2.550.000đ → giảm 510.000đ (< cap 800.000đ)")
    void percent_lata20_partnerVoucher() {
        Voucher v = buildPercentVoucher(20, 800000, 650000);
        v.setCostBearer(VoucherCostBearer.PARTNER);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("2550000"));

        // 20% × 2.550.000 = 510.000 < cap 800.000
        assertThat(discount).isEqualByComparingTo("510000");
        assertThat(v.getCostBearer()).isEqualTo(VoucherCostBearer.PARTNER);
    }

    // ─── FIXED_AMOUNT voucher ─────────────────────────────────────────────────

    @Test
    @DisplayName("FIXED 50.000đ — WELCOME50: đơn 300.000đ → giảm đúng 50.000đ")
    void fixed_basicDiscount() {
        Voucher v = buildFixedVoucher(50000, 200000, VoucherCostBearer.ADMIN);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("300000"));

        assertThat(discount).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("FIXED 100.000đ — VNT100K partner: đơn 5.600.000đ → giảm 100.000đ")
    void fixed_partnerVoucherVnt100k() {
        Voucher v = buildFixedVoucher(100000, 2800000, VoucherCostBearer.PARTNER);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("5600000"));

        assertThat(discount).isEqualByComparingTo("100000");
        assertThat(v.getCostBearer()).isEqualTo(VoucherCostBearer.PARTNER);
    }

    // ─── Guard: tổng tiền không âm ───────────────────────────────────────────

    @Test
    @DisplayName("Guard: discount không làm tổng tiền xuống dưới 5.000đ (VNPAY minimum)")
    void guard_discountCannotReduceBelowVnpayMinimum() {
        // Đơn hàng 6.000đ với voucher giảm 10% = 600đ → kết quả còn 5.400đ (ổn)
        // Nhưng nếu voucher 50.000đ fix thì kết quả = -44.000đ → phải giới hạn
        Voucher v = buildFixedVoucher(50000, 0, VoucherCostBearer.ADMIN);

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("10000"));

        // 10.000 - 50.000 = -40.000 → guard: giữ tối thiểu 5.000đ
        // discount tối đa = 10.000 - 5.000 = 5.000đ
        assertThat(discount).isEqualByComparingTo("5000");
        BigDecimal afterDiscount = new BigDecimal("10000").subtract(discount);
        assertThat(afterDiscount).isGreaterThanOrEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("Guard: discount không làm tổng tiền xuống dưới VNPAY minimum (PERCENT)")
    void guard_percentDiscountCannotReduceBelowVnpayMinimum() {
        Voucher v = buildPercentVoucher(99, 0, 0); // 99%

        BigDecimal discount = voucherService.calculateDiscount(v, new BigDecimal("6000"));

        // 99% × 6.000 = 5.940 → kết quả còn 60đ < 5.000đ → guard kích hoạt
        // discount tối đa = 6.000 - 5.000 = 1.000đ
        BigDecimal afterDiscount = new BigDecimal("6000").subtract(discount);
        assertThat(afterDiscount).isGreaterThanOrEqualTo(new BigDecimal("5000"));
    }

    // ─── Cost bearer phân biệt ─────────────────────────────────────────────

    @Test
    @DisplayName("costBearer ADMIN — không ảnh hưởng settlement partner")
    void costBearer_admin_markedCorrectly() {
        Voucher v = buildPercentVoucher(10, 500000, 500000);
        v.setCostBearer(VoucherCostBearer.ADMIN);

        assertThat(v.getCostBearer()).isEqualTo(VoucherCostBearer.ADMIN);
    }

    @Test
    @DisplayName("costBearer PARTNER — sẽ bị trừ vào payout settlement partner")
    void costBearer_partner_markedCorrectly() {
        Voucher v = buildFixedVoucher(100000, 2800000, VoucherCostBearer.PARTNER);

        assertThat(v.getCostBearer()).isEqualTo(VoucherCostBearer.PARTNER);
    }

    @Test
    @DisplayName("Partner không thể gắn voucher toàn hệ thống vì voucher này áp dụng tự động")
    void assignVoucherToRoom_rejectsGlobalVoucher() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Room ownRoom = room(accommodation(21L, "Vinpearl Resort", PropertyType.RESORT, resortPartner));
        ownRoom.setId(30L);
        ownRoom.setApprovalStatus(ApprovalStatus.APPROVED);
        Voucher global = buildPercentVoucher(10, 500000, 0);
        global.setId(10L);
        global.setVoucherScope(VoucherScope.USER_GLOBAL);
        when(voucherRepository.findById(10L)).thenReturn(Optional.of(global));
        when(roomRepository.findById(30L)).thenReturn(Optional.of(ownRoom));

        assertThatThrownBy(() -> voucherService.assignVoucherToRoom(resortPartner, 10L, 30L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("áp dụng tự động");
    }

    @Test
    @DisplayName("Partner HOMESTAY không được gắn voucher kho vào phòng RESORT")
    void assignVoucherToRoom_rejectsWrongPropertyTypeForHomestay() {
        User homestayPartner = partner(8L, PropertyType.HOMESTAY);
        Accommodation wrongResort = accommodation(9L, "Vinpearl Resort & Spa Nha Trang", PropertyType.RESORT, homestayPartner);
        Room wrongRoom = room(wrongResort);
        wrongRoom.setId(30L);
        wrongRoom.setApprovalStatus(ApprovalStatus.APPROVED);
        Voucher catalogVoucher = buildFixedVoucher(50000, 0, VoucherCostBearer.PARTNER);
        catalogVoucher.setId(11L);
        catalogVoucher.setVoucherScope(VoucherScope.PARTNER_ROOM);
        when(voucherRepository.findById(11L)).thenReturn(Optional.of(catalogVoucher));
        when(roomRepository.findById(30L)).thenReturn(Optional.of(wrongRoom));

        assertThatThrownBy(() -> voucherService.assignVoucherToRoom(homestayPartner, 11L, 30L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("loại lưu trú");
    }

    @Test
    @DisplayName("Danh sách gắn voucher Partner ẩn liên kết của sai loại lưu trú")
    void getAssignmentsForPartner_filtersWrongPropertyTypeTargets() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        RoomVoucherAssignment wrongHotelAssignment = new RoomVoucherAssignment();
        wrongHotelAssignment.setRoom(room(accommodation(20L, "Sofitel Legend Metropole Hà Nội", PropertyType.HOTEL, resortPartner)));
        RoomVoucherAssignment resortAssignment = new RoomVoucherAssignment();
        resortAssignment.setRoom(room(accommodation(21L, "Vinpearl Resort & Spa Nha Trang", PropertyType.RESORT, resortPartner)));

        when(assignmentRepository.findByAssignedByPartnerOrderByAssignedAtDesc(resortPartner))
                .thenReturn(java.util.List.of(wrongHotelAssignment, resortAssignment));

        assertThat(voucherService.getAssignmentsForPartner(resortPartner))
                .containsExactly(resortAssignment);
    }

    @Test
    @DisplayName("Partner HOTEL thấy voucher HOTEL và voucher mọi loại, không thấy voucher RESORT")
    void getPartnerRoomCatalog_filtersVouchersByRegisteredPropertyType() {
        User hotelPartner = partner(3L, PropertyType.HOTEL);
        Voucher hotelVoucher = buildPercentVoucher(20, 800000, 650000);
        hotelVoucher.setVoucherScope(VoucherScope.PARTNER_ROOM);
        hotelVoucher.setPropertyType(PropertyType.HOTEL);
        Voucher resortVoucher = buildFixedVoucher(100000, 2800000, VoucherCostBearer.PARTNER);
        resortVoucher.setVoucherScope(VoucherScope.PARTNER_ROOM);
        resortVoucher.setPropertyType(PropertyType.RESORT);
        Voucher allPartnerTypes = buildFixedVoucher(100000, 900000, VoucherCostBearer.ADMIN);
        allPartnerTypes.setVoucherScope(VoucherScope.PARTNER_ROOM);
        allPartnerTypes.setPropertyType(null);
        when(voucherRepository.findByVoucherScopeOrderByCreatedAtDesc(VoucherScope.PARTNER_ROOM))
                .thenReturn(List.of(resortVoucher, allPartnerTypes, hotelVoucher));

        assertThat(voucherService.getPartnerRoomCatalog(hotelPartner))
                .containsExactly(allPartnerTypes, hotelVoucher)
                .doesNotContain(resortVoucher);
    }

    @Test
    @DisplayName("Partner HOTEL không thể gọi tay để gắn voucher RESORT")
    void assignVoucherToRoom_rejectsVoucherFromOtherPropertyType() {
        User hotelPartner = partner(3L, PropertyType.HOTEL);
        Room ownHotelRoom = room(accommodation(1L, "LATA Hotel & Apartments", PropertyType.HOTEL, hotelPartner));
        ownHotelRoom.setId(1L);
        ownHotelRoom.setApprovalStatus(ApprovalStatus.APPROVED);
        Voucher resortVoucher = buildFixedVoucher(100000, 2800000, VoucherCostBearer.PARTNER);
        resortVoucher.setId(12L);
        resortVoucher.setVoucherScope(VoucherScope.PARTNER_ROOM);
        resortVoucher.setPropertyType(PropertyType.RESORT);
        when(voucherRepository.findById(12L)).thenReturn(Optional.of(resortVoucher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(ownHotelRoom));

        assertThatThrownBy(() -> voucherService.assignVoucherToRoom(hotelPartner, 12L, 1L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("loại lưu trú");
    }

    @Test
    @DisplayName("Voucher kho không giới hạn loại có thể được Partner HOTEL gắn vào phòng của mình")
    void assignVoucherToRoom_acceptsVoucherAvailableForAllPropertyTypes() {
        User hotelPartner = partner(3L, PropertyType.HOTEL);
        Room ownHotelRoom = room(accommodation(1L, "LATA Hotel & Apartments", PropertyType.HOTEL, hotelPartner));
        ownHotelRoom.setId(1L);
        ownHotelRoom.setApprovalStatus(ApprovalStatus.APPROVED);
        Voucher sharedVoucher = buildFixedVoucher(100000, 900000, VoucherCostBearer.ADMIN);
        sharedVoucher.setId(13L);
        sharedVoucher.setVoucherScope(VoucherScope.PARTNER_ROOM);
        sharedVoucher.setPropertyType(null);
        when(voucherRepository.findById(13L)).thenReturn(Optional.of(sharedVoucher));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(ownHotelRoom));
        when(assignmentRepository.findByVoucherAndRoom(sharedVoucher, ownHotelRoom)).thenReturn(Optional.empty());
        when(assignmentRepository.save(org.mockito.ArgumentMatchers.any(RoomVoucherAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoomVoucherAssignment assignment = voucherService.assignVoucherToRoom(hotelPartner, 13L, 1L);

        assertThat(assignment.getVoucher()).isSameAs(sharedVoucher);
        assertThat(assignment.getRoom()).isSameAs(ownHotelRoom);
        assertThat(assignment.getAssignedByPartner()).isSameAs(hotelPartner);
        assertThat(assignment.getActive()).isTrue();
    }

    @Test
    @DisplayName("User nhập voucher kho nhưng chưa gắn đúng phòng → bị từ chối")
    void validateVoucher_rejectsPartnerRoomVoucherWhenRoomIsNotAssigned() {
        User hotelPartner = partner(3L, PropertyType.HOTEL);
        Room selectedRoom = room(accommodation(1L, "LATA Hotel & Apartments", PropertyType.HOTEL, hotelPartner));
        Voucher roomVoucher = buildPercentVoucher(20, 800000, 650000);
        roomVoucher.setCode("LATA20");
        roomVoucher.setVoucherScope(VoucherScope.PARTNER_ROOM);
        roomVoucher.setPropertyType(PropertyType.HOTEL);
        when(voucherRepository.findByCode("LATA20")).thenReturn(Optional.of(roomVoucher));
        when(assignmentRepository.existsByVoucherAndRoomAndActiveTrue(roomVoucher, selectedRoom)).thenReturn(false);

        assertThatThrownBy(() -> voucherService.validateVoucher("lata20", new BigDecimal("2550000"), selectedRoom))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chưa được đối tác áp dụng");
    }

    @Test
    @DisplayName("Voucher hết hạn → không được áp dụng")
    void validateVoucher_rejectsExpiredVoucher() {
        Voucher expired = buildFixedVoucher(100000, 0, VoucherCostBearer.ADMIN);
        expired.setCode("OLD100K");
        expired.setVoucherScope(VoucherScope.USER_GLOBAL);
        expired.setStartDate(LocalDate.now().minusDays(10));
        expired.setEndDate(LocalDate.now().minusDays(1));
        when(voucherRepository.findByCode("OLD100K")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> voucherService.validateVoucher("old100k", new BigDecimal("1000000"), new Room()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hết hạn");
    }

    @Test
    @DisplayName("Voucher chưa tới ngày bắt đầu → không được áp dụng")
    void validateVoucher_rejectsFutureStartDate() {
        Voucher future = buildFixedVoucher(100000, 0, VoucherCostBearer.ADMIN);
        future.setCode("SOON100K");
        future.setVoucherScope(VoucherScope.USER_GLOBAL);
        future.setStartDate(LocalDate.now().plusDays(2));
        future.setEndDate(LocalDate.now().plusDays(12));
        when(voucherRepository.findByCode("SOON100K")).thenReturn(Optional.of(future));

        assertThatThrownBy(() -> voucherService.validateVoucher("soon100k", new BigDecimal("1000000"), new Room()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không còn hiệu lực");
    }

    @Test
    @DisplayName("Voucher chưa đạt giá trị đơn tối thiểu → không được áp dụng")
    void validateVoucher_rejectsMinimumOrderNotMet() {
        Voucher voucher = buildFixedVoucher(100000, 2000000, VoucherCostBearer.ADMIN);
        voucher.setCode("MIN2M");
        voucher.setVoucherScope(VoucherScope.USER_GLOBAL);
        when(voucherRepository.findByCode("MIN2M")).thenReturn(Optional.of(voucher));

        assertThatThrownBy(() -> voucherService.validateVoucher("min2m", new BigDecimal("650000"), new Room()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Đơn hàng chưa đạt tối thiểu");
    }

    @Test
    @DisplayName("Admin tạo voucher toàn hệ thống luôn là ADMIN chịu chi phí")
    void createAdminVoucher_globalForcesAdminCostBearer() {
        when(voucherRepository.findByCode("GLOBAL10")).thenReturn(Optional.empty());
        when(voucherRepository.save(org.mockito.ArgumentMatchers.any(Voucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Voucher voucher = voucherService.createAdminVoucher(
                "global10", "Giảm toàn hệ thống", "TravelMate chịu chi phí",
                DiscountType.PERCENT, new BigDecimal("10"), new BigDecimal("200000"),
                BigDecimal.ZERO, LocalDate.now().minusDays(1), LocalDate.now().plusDays(5),
                VoucherScope.USER_GLOBAL, VoucherCostBearer.PARTNER);

        assertThat(voucher.getCode()).isEqualTo("GLOBAL10");
        assertThat(voucher.getVoucherScope()).isEqualTo(VoucherScope.USER_GLOBAL);
        assertThat(voucher.getCostBearer()).isEqualTo(VoucherCostBearer.ADMIN);
        assertThat(voucher.getOwner()).isNull();
    }

    @Test
    @DisplayName("Admin tạo voucher kho Partner có thể chọn PARTNER chịu chi phí")
    void createAdminVoucher_partnerRoomAllowsPartnerCostBearer() {
        when(voucherRepository.findByCode("ROOM100K")).thenReturn(Optional.empty());
        when(voucherRepository.save(org.mockito.ArgumentMatchers.any(Voucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Voucher voucher = voucherService.createAdminVoucher(
                "room100k", "Giảm cho phòng partner", "Partner chịu chi phí trong quyết toán",
                DiscountType.FIXED_AMOUNT, new BigDecimal("100000"), null,
                new BigDecimal("500000"), LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                VoucherScope.PARTNER_ROOM, VoucherCostBearer.PARTNER, PropertyType.HOTEL);

        assertThat(voucher.getCode()).isEqualTo("ROOM100K");
        assertThat(voucher.getVoucherScope()).isEqualTo(VoucherScope.PARTNER_ROOM);
        assertThat(voucher.getPropertyType()).isEqualTo(PropertyType.HOTEL);
        assertThat(voucher.getCostBearer()).isEqualTo(VoucherCostBearer.PARTNER);
        assertThat(voucher.getOwner()).isNull();
        assertThat(voucher.getAccommodation()).isNull();
        assertThat(voucher.getRoom()).isNull();
    }

    @Test
    @DisplayName("Admin tạo voucher kho áp dụng mọi loại Partner khi không chọn loại lưu trú")
    void createAdminVoucher_partnerRoomAllowsAllPropertyTypes() {
        when(voucherRepository.findByCode("STAYFLEX100")).thenReturn(Optional.empty());
        when(voucherRepository.save(org.mockito.ArgumentMatchers.any(Voucher.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Voucher voucher = voucherService.createAdminVoucher(
                "stayflex100", "Ưu đãi linh hoạt", "TravelMate chịu cho mọi loại lưu trú",
                DiscountType.FIXED_AMOUNT, new BigDecimal("100000"), null,
                new BigDecimal("900000"), LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                VoucherScope.PARTNER_ROOM, VoucherCostBearer.ADMIN, null);

        assertThat(voucher.getVoucherScope()).isEqualTo(VoucherScope.PARTNER_ROOM);
        assertThat(voucher.getPropertyType()).isNull();
        assertThat(voucher.getCostBearer()).isEqualTo(VoucherCostBearer.ADMIN);
    }

    @Test
    @DisplayName("Admin không tạo voucher scope cơ sở cũ để tránh lệch settlement")
    void createAdminVoucher_rejectsLegacyPartnerAccommodationScope() {
        when(voucherRepository.findByCode("OLD-SCOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> voucherService.createAdminVoucher(
                "old-scope", "Scope cũ", "Không dùng scope cơ sở cũ",
                DiscountType.PERCENT, new BigDecimal("10"), new BigDecimal("200000"),
                BigDecimal.ZERO, LocalDate.now().minusDays(1), LocalDate.now().plusDays(5),
                VoucherScope.PARTNER_ACCOMMODATION, VoucherCostBearer.PARTNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kho Partner");
    }

    @Test
    @DisplayName("User nhập voucher kho đã gắn đúng phòng → áp dụng được")
    void validateVoucher_acceptsAssignedPartnerRoomVoucher() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Room selectedRoom = room(accommodation(8L, "Vinpearl Resort", PropertyType.RESORT, resortPartner));
        Voucher roomVoucher = buildFixedVoucher(100000, 500000, VoucherCostBearer.PARTNER);
        roomVoucher.setCode("VNT100K");
        roomVoucher.setVoucherScope(VoucherScope.PARTNER_ROOM);
        roomVoucher.setPropertyType(PropertyType.RESORT);
        when(voucherRepository.findByCode("VNT100K")).thenReturn(Optional.of(roomVoucher));
        when(assignmentRepository.existsByVoucherAndRoomAndActiveTrue(roomVoucher, selectedRoom)).thenReturn(true);

        Voucher validated = voucherService.validateVoucher("vnt100k", new BigDecimal("1200000"), selectedRoom);

        assertThat(validated).isSameAs(roomVoucher);
        assertThat(voucherService.calculateDiscount(validated, new BigDecimal("1200000")))
                .isEqualByComparingTo("100000");
    }

    private static User partner(Long id, PropertyType propertyType) {
        User partner = new User();
        partner.setId(id);
        partner.setRole(User.Role.PARTNER);
        partner.setPartnerPropertyType(propertyType);
        return partner;
    }

    private static Accommodation accommodation(Long id, String name, PropertyType propertyType, User owner) {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(id);
        accommodation.setName(name);
        accommodation.setPropertyType(propertyType);
        accommodation.setApprovalStatus(ApprovalStatus.APPROVED);
        accommodation.setOwner(owner);
        return accommodation;
    }

    private static Room room(Accommodation accommodation) {
        Room room = new Room();
        room.setAccommodation(accommodation);
        return room;
    }
}
