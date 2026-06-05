package com.travelmate.service;

import com.travelmate.config.VnpayConfigProperties;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Payment;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.entity.enums.PartnerBookingStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.RemainingPaymentStatus;
import com.travelmate.entity.enums.VoucherCostBearer;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.PaymentRepository;
import com.travelmate.repository.RoomRepository;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử logic tính tiền booking: tổng tiền, cọc 30%, thanh toán 100%.
 *
 * Không kết nối database — kiểm thử thuần logic (pure unit test).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService — Tính tiền đặt phòng")
class BookingCalculationTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private AccommodationRepository accommodationRepository;
    @Mock private VoucherService voucherService;
    @Mock private NotificationService notificationService;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository, paymentRepository, roomRepository,
                accommodationRepository, voucherService, notificationService,
                new CommissionService(),
                new VnpayConfigProperties()
        );
    }

    // ─── TotalAmount ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Tổng tiền = giá phòng × số đêm × số lượng phòng")
    void calculateTotalAmount_multipleRoomsAndNights() {
        Room room = new Room();
        room.setPricePerNight(new BigDecimal("850000"));

        BigDecimal total = bookingService.calculateTotalAmount(room, 3, 2);

        assertThat(total).isEqualByComparingTo("5100000");
    }

    @Test
    @DisplayName("Tổng tiền = giá × 1 đêm × 1 phòng (trường hợp tối thiểu)")
    void calculateTotalAmount_singleRoomOneNight() {
        Room room = new Room();
        room.setPricePerNight(new BigDecimal("650000"));

        BigDecimal total = bookingService.calculateTotalAmount(room, 1, 1);

        assertThat(total).isEqualByComparingTo("650000");
    }

    // ─── calculateNights ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Số đêm = checkOut - checkIn (3 đêm)")
    void calculateNights_returnsCorrectDiff() {
        LocalDate checkIn  = LocalDate.of(2026, 6, 1);
        LocalDate checkOut = LocalDate.of(2026, 6, 4);

        long nights = bookingService.calculateNights(checkIn, checkOut);

        assertThat(nights).isEqualTo(3);
    }

    @Test
    @DisplayName("Số đêm tối thiểu là 1 (check-in và check-out cùng ngày)")
    void calculateNights_minimumOneNight() {
        LocalDate date = LocalDate.of(2026, 6, 1);

        long nights = bookingService.calculateNights(date, date);

        assertThat(nights).isEqualTo(1);
    }

    // ─── DEPOSIT_30 ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("DEPOSIT_30: paidAmount = 30% totalAmount (làm tròn xuống)")
    void calculatePaidAmount_deposit30_is30Percent() {
        BigDecimal total = new BigDecimal("1300000"); // 2 đêm × 650.000đ

        BigDecimal paid = bookingService.calculatePaidAmount(total, PaymentOption.DEPOSIT_30);

        assertThat(paid).isEqualByComparingTo("390000"); // 1.300.000 × 30% = 390.000
    }

    @Test
    @DisplayName("DEPOSIT_30: remainingAmount = totalAmount - paidAmount")
    void calculatePaidAmount_deposit30_remainingIs70Percent() {
        BigDecimal total = new BigDecimal("1300000");
        BigDecimal paid  = bookingService.calculatePaidAmount(total, PaymentOption.DEPOSIT_30);

        BigDecimal remaining = total.subtract(paid);

        assertThat(paid).isEqualByComparingTo("390000");       // 30%
        assertThat(remaining).isEqualByComparingTo("910000");  // 70%
        assertThat(paid.add(remaining)).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("DEPOSIT_30: làm tròn xuống (floor) không vượt quá 30%")
    void calculatePaidAmount_deposit30_floorRounding() {
        BigDecimal total = new BigDecimal("1000001"); // số lẻ

        BigDecimal paid = bookingService.calculatePaidAmount(total, PaymentOption.DEPOSIT_30);

        // 1.000.001 × 0.3 = 300.000,3 → floor → 300.000
        assertThat(paid).isEqualByComparingTo("300000");
        assertThat(paid).isLessThanOrEqualTo(total.multiply(new BigDecimal("0.3")));
    }

    // ─── FULL_PAYMENT ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("FULL_PAYMENT: paidAmount = 100% totalAmount")
    void calculatePaidAmount_fullPayment_is100Percent() {
        BigDecimal total = new BigDecimal("4950000");

        BigDecimal paid = bookingService.calculatePaidAmount(total, PaymentOption.FULL_PAYMENT);

        assertThat(paid).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("FULL_PAYMENT: remainingAmount = 0")
    void calculatePaidAmount_fullPayment_remainingIsZero() {
        BigDecimal total = new BigDecimal("13500000");
        BigDecimal paid  = bookingService.calculatePaidAmount(total, PaymentOption.FULL_PAYMENT);

        BigDecimal remaining = total.subtract(paid);

        assertThat(remaining).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─── Kịch bản thực tế (dữ liệu mẫu) ────────────────────────────────────

    @Test
    @DisplayName("BK-TLP-STD-0001: 2 đêm × 480.000đ × 1 phòng — cọc 30% = 288.000đ")
    void realScenario_tulipStandardDeposit30() {
        Room room = new Room();
        room.setPricePerNight(new BigDecimal("480000"));

        BigDecimal total = bookingService.calculateTotalAmount(room, 2, 1);
        BigDecimal paid  = bookingService.calculatePaidAmount(total, PaymentOption.DEPOSIT_30);
        BigDecimal remaining = total.subtract(paid);

        assertThat(total).isEqualByComparingTo("960000");
        assertThat(paid).isEqualByComparingTo("288000");
        assertThat(remaining).isEqualByComparingTo("672000");
    }

    @Test
    @DisplayName("BK-TMG-PRE-0001: 3 đêm × 1.650.000đ × 1 phòng — 100% = 4.950.000đ")
    void realScenario_tmgPremiumFullPayment() {
        Room room = new Room();
        room.setPricePerNight(new BigDecimal("1650000"));

        BigDecimal total = bookingService.calculateTotalAmount(room, 3, 1);
        BigDecimal paid  = bookingService.calculatePaidAmount(total, PaymentOption.FULL_PAYMENT);

        assertThat(total).isEqualByComparingTo("4950000");
        assertThat(paid).isEqualByComparingTo("4950000");
    }

    @Test
    @DisplayName("Partner RESORT không được tạo booking trực tiếp cho listing HOTEL dù owner bị gán nhầm")
    void createDirectBooking_rejectsWrongPropertyTypeEvenWhenOwnerMatches() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Accommodation wrongHotel = accommodation(20L, "InterContinental Nha Trang", PropertyType.HOTEL, resortPartner);
        Room room = room(30L, "ICN-STD", wrongHotel);

        assertThatThrownBy(() -> bookingService.createDirectBooking(
                resortPartner, room, wrongHotel,
                "Khách test", "0901234567", "guest@example.com",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                1, 0, 1, null))
                .hasMessageContaining("Tài khoản RESORT");
    }

    @Test
    @DisplayName("Danh sách booking Partner chỉ giữ booking đúng loại lưu trú đã đăng ký")
    void getAllBookingsForPartner_filtersWrongPropertyType() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Accommodation wrongHotel = accommodation(20L, "Sofitel Legend Metropole Hà Nội", PropertyType.HOTEL, resortPartner);
        Accommodation resort = accommodation(21L, "Vinpearl Resort & Spa Nha Trang", PropertyType.RESORT, resortPartner);
        Booking hotelBooking = booking(wrongHotel);
        Booking resortBooking = booking(resort);

        when(bookingRepository.findAllByAccommodationOwnerOrderByCreatedAtDesc(resortPartner))
                .thenReturn(List.of(hotelBooking, resortBooking));

        assertThat(bookingService.getAllBookingsForPartner(resortPartner))
                .containsExactly(resortBooking);
    }

    @Test
    @DisplayName("Partner không được giữ booking khác partnerPropertyType dù owner khớp")
    void confirmBookingHold_rejectsWrongPropertyTypeEvenWhenOwnerMatches() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Accommodation wrongVilla = accommodation(22L, "The Anam Villa Nha Trang", PropertyType.VILLA, resortPartner);
        Booking booking = booking(wrongVilla);
        booking.setId(55L);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPartnerStatus(PartnerBookingStatus.PENDING_PARTNER_CONFIRMATION);

        when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBookingHoldByPartner(55L, resortPartner))
                .hasMessageContaining("Tài khoản RESORT");
    }

    @Test
    @DisplayName("Partner Resort không thể gọi check-in thường để bỏ qua xác nhận thu 70% của đơn cọc")
    void checkInByPartner_rejectsOnlineDepositWithoutRemainingConfirmation() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Accommodation resort = accommodation(21L, "Vinpearl Resort & Spa Nha Trang", PropertyType.RESORT, resortPartner);
        Booking booking = booking(resort);
        booking.setId(61L);
        booking.setRoom(room(31L, "VNT-DLX", resort));
        booking.setPaymentOption(PaymentOption.DEPOSIT_30);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        booking.setBookingSource(BookingSource.ONLINE);
        booking.setRemainingPaymentStatus(RemainingPaymentStatus.UNPAID);

        when(bookingRepository.findById(61L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.checkInByPartner(61L, resortPartner))
                .hasMessageContaining("xác nhận đã thu đủ 70%");
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Partner Resort check-in đơn cọc qua xác nhận sẽ ghi nhận đã thu 70% tại cơ sở")
    void checkInWithRemainingConfirm_recordsDepositCollectionForResort() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Accommodation resort = accommodation(21L, "Vinpearl Resort & Spa Nha Trang", PropertyType.RESORT, resortPartner);
        Booking booking = booking(resort);
        booking.setId(62L);
        booking.setRoom(room(31L, "VNT-DLX", resort));
        booking.setPaymentOption(PaymentOption.DEPOSIT_30);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        booking.setBookingSource(BookingSource.ONLINE);
        booking.setRemainingPaymentStatus(RemainingPaymentStatus.UNPAID);

        when(bookingRepository.findById(62L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.checkInWithRemainingConfirmByPartner(
                62L, resortPartner, true, "Đã thu bằng thẻ tại quầy Resort");

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(result.getRemainingPaymentStatus()).isEqualTo(RemainingPaymentStatus.PAID_AT_PROPERTY);
        assertThat(result.getRemainingPaymentNote()).contains("thẻ");
        assertThat(result.getNote()).contains("Đã thu đủ khoản còn lại");
    }

    @Test
    @DisplayName("DEPOSIT_30 cho phép voucher Partner đúng 10% tổng đơn")
    void deposit30_partnerVoucherAtTenPercentIsAllowed() {
        assertThat(bookingService.isPartnerVoucherAllowedForDeposit(
                new BigDecimal("240000"),
                new BigDecimal("2400000"))).isTrue();

        assertThatCode(() -> bookingService.validateDepositPartnerVoucher(
                PaymentOption.DEPOSIT_30,
                VoucherCostBearer.PARTNER,
                new BigDecimal("240000"),
                new BigDecimal("2400000"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("DEPOSIT_30 chỉ chặn voucher Partner lớn hơn 10% tổng đơn")
    void deposit30_partnerVoucherAboveTenPercentIsRejected() {
        assertThatThrownBy(() -> bookingService.validateDepositPartnerVoucher(
                PaymentOption.DEPOSIT_30,
                VoucherCostBearer.PARTNER,
                new BigDecimal("240001"),
                new BigDecimal("2400000")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("tối đa 10%");
    }

    @Test
    @DisplayName("DEPOSIT_30 cho phép payout bằng 0 nhưng không cho âm")
    void deposit30_partnerPayoutGuard_allowsZeroAndRejectsNegative() {
        Accommodation hotel = accommodation(31L, "Hotel commission cao", PropertyType.HOTEL, partner(31L, PropertyType.HOTEL));
        Room vip20 = room(41L, "H20-VIP", hotel);
        vip20.setCommissionRateOverride(new BigDecimal("20.00"));
        Room custom25 = room(42L, "H25-VIP", hotel);
        custom25.setCommissionRateOverride(new BigDecimal("25.00"));

        assertThat(bookingService.calculateDepositPartnerPayoutFromTravelMate(
                vip20,
                VoucherCostBearer.PARTNER,
                new BigDecimal("240000"),
                new BigDecimal("2400000"))).isEqualByComparingTo("0");

        assertThatCode(() -> bookingService.validateDepositPartnerPayout(
                PaymentOption.DEPOSIT_30,
                vip20,
                VoucherCostBearer.PARTNER,
                new BigDecimal("240000"),
                new BigDecimal("2400000"))).doesNotThrowAnyException();

        assertThatThrownBy(() -> bookingService.validateDepositPartnerPayout(
                PaymentOption.DEPOSIT_30,
                custom25,
                VoucherCostBearer.PARTNER,
                new BigDecimal("240000"),
                new BigDecimal("2400000")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không đủ");
    }

    @Test
    @DisplayName("FULL_PAYMENT chặn voucher Partner làm quyết toán âm")
    void fullPayment_partnerPayoutGuardRejectsNegative() {
        Accommodation hotel = accommodation(32L, "TravelMate City Hotel", PropertyType.HOTEL, partner(32L, PropertyType.HOTEL));
        Room room = room(43L, "TMH-STD", hotel);

        assertThat(bookingService.calculatePartnerPayoutFromTravelMate(
                PaymentOption.FULL_PAYMENT,
                room,
                VoucherCostBearer.PARTNER,
                new BigDecimal("150000"),
                new BigDecimal("1000000"))).isEqualByComparingTo("550000");

        assertThatThrownBy(() -> bookingService.validatePartnerPayout(
                PaymentOption.FULL_PAYMENT,
                room,
                VoucherCostBearer.PARTNER,
                new BigDecimal("850000"),
                new BigDecimal("1000000")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("bị âm");
    }

    @Test
    @DisplayName("Partner Resort không bị yêu cầu thu lại 70% khi khoản còn lại đã ghi nhận")
    void checkInWithRemainingConfirm_doesNotRequireCollectionAgainWhenAlreadyPaid() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Accommodation resort = accommodation(21L, "Vinpearl Resort & Spa Nha Trang", PropertyType.RESORT, resortPartner);
        Booking booking = booking(resort);
        booking.setId(63L);
        booking.setRoom(room(31L, "VNT-DLX", resort));
        booking.setPaymentOption(PaymentOption.DEPOSIT_30);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        booking.setBookingSource(BookingSource.ONLINE);
        booking.setRemainingPaymentStatus(RemainingPaymentStatus.PAID_AT_PROPERTY);
        booking.setRemainingPaymentNote("Đã thu trước khi khách đến");

        when(bookingRepository.findById(63L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.checkInWithRemainingConfirmByPartner(
                63L, resortPartner, false, null);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(result.getRemainingPaymentStatus()).isEqualTo(RemainingPaymentStatus.PAID_AT_PROPERTY);
        assertThat(result.getRemainingPaymentNote()).isEqualTo("Đã thu trước khi khách đến");
    }

    @Test
    @DisplayName("Partner Homestay không thấy đơn online đang chờ Admin duyệt nhưng vẫn thấy lịch tự tạo")
    void getAllBookingsForPartner_hidesPendingOnlineHomestayAndKeepsOperations() {
        User homestayPartner = partner(8L, PropertyType.HOMESTAY);
        Accommodation homestay = accommodation(7L, "Mộc Nhiên Garden Homestay Đà Lạt",
                PropertyType.HOMESTAY, homestayPartner);
        Booking pendingAdmin = booking(homestay);
        pendingAdmin.setBookingStatus(BookingStatus.PENDING_ADMIN_APPROVAL);
        pendingAdmin.setBookingSource(BookingSource.ONLINE);
        Booking approvedOnline = booking(homestay);
        approvedOnline.setBookingSource(BookingSource.ONLINE);
        Booking direct = booking(homestay);
        direct.setBookingSource(BookingSource.DIRECT);
        Booking block = booking(homestay);
        block.setBookingSource(BookingSource.MANUAL_BLOCK);

        when(bookingRepository.findAllByAccommodationOwnerOrderByCreatedAtDesc(homestayPartner))
                .thenReturn(List.of(pendingAdmin, approvedOnline, direct, block));

        assertThat(bookingService.getAllBookingsForPartner(homestayPartner))
                .containsExactly(approvedOnline, direct, block)
                .doesNotContain(pendingAdmin);
    }

    @Test
    @DisplayName("Partner Homestay không mở được chi tiết giao dịch ngoại lệ chưa đối soát")
    void getVisibleBookingForPartner_rejectsPendingOnlineHomestayDetail() {
        User homestayPartner = partner(8L, PropertyType.HOMESTAY);
        Accommodation homestay = accommodation(7L, "Mộc Nhiên Garden Homestay Đà Lạt",
                PropertyType.HOMESTAY, homestayPartner);
        Booking pendingAdmin = booking(homestay);
        pendingAdmin.setId(64L);
        pendingAdmin.setBookingStatus(BookingStatus.PENDING_ADMIN_APPROVAL);
        pendingAdmin.setBookingSource(BookingSource.ONLINE);

        when(bookingRepository.findById(64L)).thenReturn(Optional.of(pendingAdmin));

        assertThatThrownBy(() -> bookingService.getVisibleBookingForPartner(64L, homestayPartner))
                .hasMessageContaining("sau khi VNPAY ghi nhận");
    }

    @Test
    @DisplayName("Partner Homestay check-in đơn cọc phải ghi nhận thu 70% tại cơ sở")
    void checkInWithRemainingConfirm_recordsDepositCollectionForHomestay() {
        User homestayPartner = partner(8L, PropertyType.HOMESTAY);
        Accommodation homestay = accommodation(7L, "Mộc Nhiên Garden Homestay Đà Lạt",
                PropertyType.HOMESTAY, homestayPartner);
        Booking booking = booking(homestay);
        booking.setId(65L);
        booking.setRoom(room(23L, "MND-ATT", homestay));
        booking.setPaymentOption(PaymentOption.DEPOSIT_30);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        booking.setBookingSource(BookingSource.ONLINE);
        booking.setRemainingPaymentStatus(RemainingPaymentStatus.UNPAID);

        when(bookingRepository.findById(65L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.checkInWithRemainingConfirmByPartner(
                65L, homestayPartner, true, "Thu tiền mặt tại Homestay");

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(result.getRemainingPaymentStatus()).isEqualTo(RemainingPaymentStatus.PAID_AT_PROPERTY);
        assertThat(result.getRemainingPaymentNote()).contains("Homestay");
    }

    @Test
    @DisplayName("Partner Homestay check-in đơn thanh toán 100% không yêu cầu thu thêm 70%")
    void checkInByPartner_allowsFullPaymentHomestayWithoutRemainingCollection() {
        User homestayPartner = partner(8L, PropertyType.HOMESTAY);
        Accommodation homestay = accommodation(7L, "Mộc Nhiên Garden Homestay Đà Lạt",
                PropertyType.HOMESTAY, homestayPartner);
        Booking booking = booking(homestay);
        booking.setId(66L);
        booking.setRoom(room(23L, "MND-ATT", homestay));
        booking.setPaymentOption(PaymentOption.FULL_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        booking.setBookingSource(BookingSource.ONLINE);
        booking.setRemainingPaymentStatus(RemainingPaymentStatus.NOT_REQUIRED);

        when(bookingRepository.findById(66L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.checkInByPartner(66L, homestayPartner);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CHECKED_IN);
        assertThat(result.getRemainingPaymentStatus()).isEqualTo(RemainingPaymentStatus.NOT_REQUIRED);
    }

    @Test
    @DisplayName("Partner Homestay báo no-show đơn cọc chỉ giữ cọc và mở lại quota phòng")
    void markNoShowByPartner_homestayDepositForfeitsDepositAndRestoresQuota() {
        User homestayPartner = partner(8L, PropertyType.HOMESTAY);
        Accommodation homestay = accommodation(6L, "Hoa Lư Riverside Homestay",
                PropertyType.HOMESTAY, homestayPartner);
        Room room = room(19L, "HLR-STD", homestay);
        room.setAvailableQuantity(2);
        Booking booking = booking(homestay);
        booking.setId(67L);
        booking.setRoom(room);
        booking.setRoomQuantity(1);
        booking.setPaymentOption(PaymentOption.DEPOSIT_30);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPaidAmount(new BigDecimal("192000"));
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        booking.setBookingSource(BookingSource.ONLINE);
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.APPROVED);

        when(bookingRepository.findById(67L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.markNoShowByPartner(67L, homestayPartner);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.NO_SHOW);
        assertThat(result.getRefundAmount()).isEqualByComparingTo("0");
        assertThat(result.getCancellationFee()).isEqualByComparingTo("192000");
        assertThat(result.getOnsiteAmountSnapshot()).isEqualByComparingTo("0");
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.DEPOSIT_FORFEITED);
        assertThat(room.getAvailableQuantity()).isEqualTo(3);
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("Tổng doanh thu hệ thống không cộng payment legacy của booking trực tiếp")
    void calculateApprovedRevenue_excludesDirectPaymentRecords() {
        Booking onlineBooking = new Booking();
        onlineBooking.setBookingSource(BookingSource.ONLINE);
        Payment onlinePayment = new Payment();
        onlinePayment.setBooking(onlineBooking);
        onlinePayment.setAmount(new BigDecimal("960000"));
        Booking directBooking = new Booking();
        directBooking.setBookingSource(BookingSource.DIRECT);
        Payment directPayment = new Payment();
        directPayment.setBooking(directBooking);
        directPayment.setAmount(new BigDecimal("780000"));

        when(paymentRepository.findByPaymentStatusIn(any()))
                .thenReturn(List.of(onlinePayment, directPayment));

        assertThat(bookingService.calculateApprovedRevenue()).isEqualByComparingTo("960000");
    }

    @Test
    @DisplayName("User sửa URL đặt phòng chưa approved → backend chặn")
    void createBooking_rejectsPendingRoomEvenIfUserKnowsRoomId() {
        Accommodation accommodation = accommodation(1L, "LATA Hotel & Apartments", PropertyType.HOTEL, null);
        Room pendingRoom = room(1L, "LATA-PENDING", accommodation);
        pendingRoom.setApprovalStatus(ApprovalStatus.PENDING);

        assertThatThrownBy(() -> bookingService.createBooking(
                new User(), pendingRoom, accommodation,
                "Nguyen Van A", "0901234567", "a@example.com",
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(4),
                1, 0, 1, PaymentOption.FULL_PAYMENT))
                .hasMessageContaining("chưa được Admin duyệt");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("User sửa URL roomId của cơ sở khác → backend chặn")
    void createBooking_rejectsRoomThatDoesNotBelongToSelectedAccommodation() {
        Accommodation selectedAccommodation = accommodation(1L, "LATA Hotel & Apartments", PropertyType.HOTEL, null);
        Accommodation otherAccommodation = accommodation(2L, "Tulip City Hotel", PropertyType.HOTEL, null);
        Room otherRoom = room(2L, "TLP-STD", otherAccommodation);

        assertThatThrownBy(() -> bookingService.createBooking(
                new User(), otherRoom, selectedAccommodation,
                "Nguyen Van A", "0901234567", "a@example.com",
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(4),
                1, 0, 1, PaymentOption.FULL_PAYMENT))
                .hasMessageContaining("không thuộc nơi lưu trú");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Overbooking: phòng cuối đã bị giữ trong khoảng ngày → booking mới bị chặn")
    void createBooking_rechecksAvailabilityBeforeCreatingPayment() {
        Accommodation accommodation = accommodation(1L, "LATA Hotel & Apartments", PropertyType.HOTEL, null);
        Room room = room(1L, "LATA-STD", accommodation);
        room.setAvailableQuantity(0);
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(bookingRepository.sumActiveQtyForRoom(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyList())).thenReturn(1);
        when(bookingRepository.sumOverlappingQtyForRoom(
                org.mockito.ArgumentMatchers.anyLong(), any(), any(), org.mockito.ArgumentMatchers.anyList())).thenReturn(1);

        assertThatThrownBy(() -> bookingService.createBooking(
                new User(), room, accommodation,
                "Nguyen Van A", "0901234567", "a@example.com",
                LocalDate.now().plusDays(3), LocalDate.now().plusDays(4),
                1, 0, 1, PaymentOption.FULL_PAYMENT))
                .hasMessageContaining("vừa hết chỗ");

        verify(bookingRepository, never()).save(any(Booking.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Đặt lại đơn đã hủy: prefill thông tin và đổi ngày cũ sang ngày mới an toàn")
    void prepareRebook_cancelledPastBookingPrefillsSafeDatesAndIndependentNotice() {
        User user = user(109L);
        Accommodation accommodation = accommodation(31L, "LATA Hotel & Apartments", PropertyType.HOTEL, null);
        Room room = room(91L, "LATA-STD", accommodation);
        Booking booking = booking(accommodation);
        booking.setId(91L);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.DEPOSIT_FORFEITED);
        booking.setPaymentOption(PaymentOption.DEPOSIT_30);
        booking.setCheckIn(LocalDate.now().minusDays(4));
        booking.setCheckOut(LocalDate.now().minusDays(2));
        booking.setAdults(2);
        booking.setChildren(1);
        booking.setRoomQuantity(2);

        when(bookingRepository.findById(91L)).thenReturn(Optional.of(booking));

        BookingService.RebookDraft draft = bookingService.prepareRebook(91L, user);

        assertThat(draft.roomId()).isEqualTo(91L);
        assertThat(draft.checkIn()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(draft.checkOut()).isEqualTo(LocalDate.now().plusDays(2));
        assertThat(draft.adults()).isEqualTo(2);
        assertThat(draft.children()).isEqualTo(1);
        assertThat(draft.rooms()).isEqualTo(2);
        assertThat(draft.notice()).contains("Khoản cọc", "không được chuyển");
        verify(paymentRepository, never()).findByBooking(any(Booking.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Đặt lại đơn cũ: chặn nếu không phải chủ đơn")
    void prepareRebook_rejectsBookingOwnedByAnotherUser() {
        Booking booking = new Booking();
        booking.setId(92L);
        booking.setUser(user(201L));
        booking.setBookingStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findById(92L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.prepareRebook(92L, user(202L)))
                .hasMessageContaining("không có quyền");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Đặt lại đơn cũ: chặn nếu phòng/căn đã tạm ngừng mở bán")
    void prepareRebook_rejectsStoppedSellingRoom() {
        User user = user(110L);
        Accommodation accommodation = accommodation(32L, "The Anam Villa Nha Trang", PropertyType.VILLA, null);
        Room room = room(93L, "ANM-GDN", accommodation);
        room.setAvailableForBooking(false);
        Booking booking = booking(accommodation);
        booking.setId(93L);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setBookingStatus(BookingStatus.NO_SHOW);

        when(bookingRepository.findById(93L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.prepareRebook(93L, user))
                .hasMessageContaining("tạm ngừng mở bán");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("User hủy đơn chưa thanh toán: hủy thẳng và mở lại quota")
    void cancelBooking_pendingPaymentCancelsWithoutRefund() {
        User user = user(101L);
        Room room = room(71L, "LATA-STD", accommodation(1L, "LATA Hotel & Apartments", PropertyType.HOTEL, null));
        room.setAvailableQuantity(1);
        Booking booking = booking(room.getAccommodation());
        booking.setId(71L);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setRoomQuantity(2);
        booking.setBookingStatus(BookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        booking.setPaidAmount(BigDecimal.ZERO);
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        when(bookingRepository.findById(71L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancelBooking(71L, user);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(room.getAvailableQuantity()).isEqualTo(3);
        verify(roomRepository).save(room);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("User hủy đơn đã thanh toán 100%: hoàn 70% và giữ 30% phí hủy")
    void cancelBooking_confirmedFullPaymentMovesToRefundPending() {
        User user = user(102L);
        Room room = room(72L, "TLP-SUP", accommodation(2L, "Tulip City Hotel", PropertyType.HOTEL, null));
        room.setAvailableQuantity(0);
        Booking booking = booking(room.getAccommodation());
        booking.setId(72L);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setRoomQuantity(1);
        booking.setPaymentOption(PaymentOption.FULL_PAYMENT);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPaidAmount(new BigDecimal("1560000"));
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.APPROVED);

        when(bookingRepository.findById(72L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancelBooking(72L, user);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(result.getRefundAmount()).isEqualByComparingTo("1092000");
        assertThat(result.getCancellationFee()).isEqualByComparingTo("468000");
        assertThat(room.getAvailableQuantity()).isEqualTo(1);
        verify(roomRepository).save(room);
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("User hủy đơn đã cọc 30%: mất cọc và không tạo yêu cầu hoàn tiền")
    void cancelBooking_confirmedDepositForfeitsDeposit() {
        User user = user(103L);
        Room room = room(73L, "HLR-STD", accommodation(6L, "Hoa Lu Riverside Homestay", PropertyType.HOMESTAY, null));
        room.setAvailableQuantity(2);
        Booking booking = booking(room.getAccommodation());
        booking.setId(73L);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setRoomQuantity(1);
        booking.setPaymentOption(PaymentOption.DEPOSIT_30);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.APPROVED);
        booking.setPaidAmount(new BigDecimal("288000"));
        booking.setRemainingAmount(new BigDecimal("672000"));
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.APPROVED);

        when(bookingRepository.findById(73L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancelBooking(73L, user);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.DEPOSIT_FORFEITED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.DEPOSIT_FORFEITED);
        assertThat(result.getPaidAmount()).isEqualByComparingTo("288000");
        assertThat(result.getRefundAmount()).isEqualByComparingTo("0");
        assertThat(result.getCancellationFee()).isEqualByComparingTo("288000");
        assertThat(result.getOnsiteAmountSnapshot()).isEqualByComparingTo("0");
        assertThat(room.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("User hủy đơn có paidAmount > 0: vẫn chuyển hoàn tiền dù trạng thái payment cũ bị lệch")
    void cancelBooking_paidAmountGuardMovesToRefundPending() {
        User user = user(105L);
        Room room = room(76L, "ANM-GDN", accommodation(4L, "The Anam Villa Nha Trang", PropertyType.VILLA, null));
        room.setAvailableQuantity(0);
        Booking booking = booking(room.getAccommodation());
        booking.setId(76L);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setRoomQuantity(1);
        booking.setPaymentOption(PaymentOption.FULL_PAYMENT);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        booking.setPaidAmount(new BigDecimal("3200000"));
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        when(bookingRepository.findById(76L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.cancelBooking(76L, user);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(room.getAvailableQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("User bấm hủy lần hai: không cập nhật lại booking/payment/quota")
    void cancelBooking_alreadyCancelledIsRejectedWithoutSideEffects() {
        User user = user(104L);
        Room room = room(74L, "VNT-DLX", accommodation(8L, "Vinpearl Resort", PropertyType.RESORT, null));
        room.setAvailableQuantity(1);
        Booking booking = booking(room.getAccommodation());
        booking.setId(74L);
        booking.setUser(user);
        booking.setRoom(room);
        booking.setRoomQuantity(1);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);

        when(bookingRepository.findById(74L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(74L, user))
                .hasMessageContaining("Chỉ có thể hủy đơn");
        assertThat(room.getAvailableQuantity()).isEqualTo(1);
        verify(roomRepository, never()).save(any(Room.class));
        verify(paymentRepository, never()).findByBooking(any(Booking.class));
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Booking đã hủy không cho Partner ghi nhận giữ legacy, check-in hoặc báo no-show")
    void partnerActions_rejectCancelledBooking() {
        User partner = partner(44L, PropertyType.HOTEL);
        Accommodation accommodation = accommodation(44L, "An Nam Boutique Hotel", PropertyType.HOTEL, partner);
        Booking booking = booking(accommodation);
        booking.setId(75L);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        booking.setPartnerStatus(PartnerBookingStatus.PENDING_PARTNER_CONFIRMATION);

        when(bookingRepository.findById(75L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmBookingHoldByPartner(75L, partner))
                .hasMessageContaining("CONFIRMED");
        assertThatThrownBy(() -> bookingService.checkInByPartner(75L, partner))
                .hasMessageContaining("CONFIRMED");
        assertThatThrownBy(() -> bookingService.markNoShowByPartner(75L, partner))
                .hasMessageContaining("CONFIRMED");
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Admin xác nhận hoàn đơn FULL_PAYMENT: payment REFUNDED, booking vẫn CANCELLED")
    void markRefundedByAdmin_keepsCancelledBookingAndUpdatesPayment() {
        Booking booking = new Booking();
        booking.setId(77L);
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        booking.setRefundAmount(new BigDecimal("1092000"));
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        payment.setNote("Chờ hoàn 70% theo chính sách.");

        when(bookingRepository.findById(77L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.markRefundedByAdmin(77L, "Đã đối soát chuyển khoản");

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.getRefundAmount()).isEqualByComparingTo("1092000");
        assertThat(result.getNote()).contains("Đã đối soát chuyển khoản");
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getNote()).contains("Đã ghi nhận hoàn tiền");
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("Admin không thể mark refunded khi booking chưa ở REFUND_PENDING")
    void markRefundedByAdmin_rejectsBookingThatIsNotRefundPending() {
        Booking booking = new Booking();
        booking.setId(84L);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.APPROVED);

        when(bookingRepository.findById(84L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.markRefundedByAdmin(84L, "Test hoàn sai trạng thái"))
                .hasMessageContaining("REFUND_PENDING");
        verify(bookingRepository, never()).save(any(Booking.class));
        verify(paymentRepository, never()).findByBooking(any(Booking.class));
    }

    @Test
    @DisplayName("User không thể hủy booking sau check-in hoặc sau khi hoàn tất")
    void cancelBooking_rejectsCheckedInAndCompletedBookings() {
        User user = user(106L);
        Booking checkedIn = new Booking();
        checkedIn.setUser(user);
        checkedIn.setBookingStatus(BookingStatus.CHECKED_IN);
        Booking completed = new Booking();
        completed.setUser(user);
        completed.setBookingStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findById(78L)).thenReturn(Optional.of(checkedIn));
        when(bookingRepository.findById(79L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> bookingService.cancelBooking(78L, user))
                .hasMessageContaining("đã check-in");
        assertThatThrownBy(() -> bookingService.cancelBooking(79L, user))
                .hasMessageContaining("đã hoàn tất");
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("User không thể hủy booking thuộc tài khoản khác")
    void cancelBooking_rejectsBookingOwnedByAnotherUser() {
        Booking booking = new Booking();
        booking.setUser(user(107L));
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(80L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(80L, user(108L)))
                .hasMessageContaining("không có quyền");
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Partner không thể báo no-show đơn FULL_PAYMENT và không thể báo trước khi giữ phòng")
    void markNoShowByPartner_enforcesDepositAndPartnerConfirmationRules() {
        User hotelPartner = partner(45L, PropertyType.HOTEL);
        Accommodation hotel = accommodation(45L, "TravelMate Grand Hotel", PropertyType.HOTEL, hotelPartner);
        Booking fullPayment = booking(hotel);
        fullPayment.setId(81L);
        fullPayment.setPaymentOption(PaymentOption.FULL_PAYMENT);
        fullPayment.setPaymentStatus(PaymentStatus.APPROVED);
        fullPayment.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        fullPayment.setBookingSource(BookingSource.ONLINE);
        Booking unconfirmedDeposit = booking(hotel);
        unconfirmedDeposit.setId(82L);
        unconfirmedDeposit.setPaymentOption(PaymentOption.DEPOSIT_30);
        unconfirmedDeposit.setPartnerStatus(PartnerBookingStatus.PENDING_PARTNER_CONFIRMATION);

        when(bookingRepository.findById(81L)).thenReturn(Optional.of(fullPayment));
        when(bookingRepository.findById(82L)).thenReturn(Optional.of(unconfirmedDeposit));

        assertThatThrownBy(() -> bookingService.markNoShowByPartner(81L, hotelPartner))
                .hasMessageContaining("thanh toán 100%");
        assertThatThrownBy(() -> bookingService.markNoShowByPartner(82L, hotelPartner))
                .hasMessageContaining("giữ phòng");
    }

    @Test
    @DisplayName("Partner/Admin chưa thể check-in hoặc báo no-show trước ngày nhận phòng")
    void partnerAndAdminActions_rejectBeforeCheckInDate() {
        User hotelPartner = partner(47L, PropertyType.HOTEL);
        Accommodation hotel = accommodation(47L, "TravelMate City Hotel", PropertyType.HOTEL, hotelPartner);
        Room room = room(90L, "TMH-STD", hotel);

        Booking earlyCheckIn = booking(hotel);
        earlyCheckIn.setId(90L);
        earlyCheckIn.setRoom(room);
        earlyCheckIn.setCheckIn(LocalDate.now().plusDays(1));
        earlyCheckIn.setPaymentOption(PaymentOption.FULL_PAYMENT);
        earlyCheckIn.setPaymentStatus(PaymentStatus.APPROVED);
        earlyCheckIn.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        earlyCheckIn.setBookingSource(BookingSource.ONLINE);

        Booking earlyPartnerNoShow = booking(hotel);
        earlyPartnerNoShow.setId(91L);
        earlyPartnerNoShow.setRoom(room);
        earlyPartnerNoShow.setCheckIn(LocalDate.now().plusDays(1));
        earlyPartnerNoShow.setPaymentOption(PaymentOption.DEPOSIT_30);
        earlyPartnerNoShow.setPaymentStatus(PaymentStatus.APPROVED);
        earlyPartnerNoShow.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        earlyPartnerNoShow.setBookingSource(BookingSource.ONLINE);

        Booking earlyAdminNoShow = booking(hotel);
        earlyAdminNoShow.setId(92L);
        earlyAdminNoShow.setRoom(room);
        earlyAdminNoShow.setCheckIn(LocalDate.now().plusDays(1));
        earlyAdminNoShow.setPaymentOption(PaymentOption.DEPOSIT_30);
        earlyAdminNoShow.setPaymentStatus(PaymentStatus.APPROVED);

        when(bookingRepository.findById(90L)).thenReturn(Optional.of(earlyCheckIn));
        when(bookingRepository.findById(91L)).thenReturn(Optional.of(earlyPartnerNoShow));
        when(bookingRepository.findById(92L)).thenReturn(Optional.of(earlyAdminNoShow));

        assertThatThrownBy(() -> bookingService.checkInByPartner(90L, hotelPartner))
                .hasMessageContaining("Chưa tới ngày nhận phòng");
        assertThatThrownBy(() -> bookingService.markNoShowByPartner(91L, hotelPartner))
                .hasMessageContaining("Chưa tới ngày nhận phòng");
        assertThatThrownBy(() -> bookingService.markNoShow(92L))
                .hasMessageContaining("Chưa tới ngày nhận phòng");
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Partner check-out đơn cọc đã thu tại cơ sở: hoàn tất và mở lại quota")
    void markCompletedByPartner_completesPaidDepositAndRestoresRoomQuota() {
        User partner = partner(46L, PropertyType.HOMESTAY);
        Accommodation homestay = accommodation(46L, "Green Hills Homestay", PropertyType.HOMESTAY, partner);
        Room room = room(83L, "GHH-FAM", homestay);
        room.setAvailableQuantity(1);
        Booking booking = booking(homestay);
        booking.setId(83L);
        booking.setRoom(room);
        booking.setRoomQuantity(1);
        booking.setBookingStatus(BookingStatus.CHECKED_IN);
        booking.setPaymentOption(PaymentOption.DEPOSIT_30);
        booking.setBookingSource(BookingSource.ONLINE);
        booking.setPartnerStatus(PartnerBookingStatus.PARTNER_CONFIRMED);
        booking.setRemainingPaymentStatus(RemainingPaymentStatus.PAID_AT_PROPERTY);

        when(bookingRepository.findById(83L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking result = bookingService.markCompletedByPartner(83L, partner);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(result.getPartnerStatus()).isEqualTo(PartnerBookingStatus.PARTNER_COMPLETED);
        assertThat(room.getAvailableQuantity()).isEqualTo(2);
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("Admin duyệt giao dịch ngoại lệ cũng tự giữ phòng/căn")
    void approveBookingByAdminAutoHoldsRoomForPartner() {
        User partner = partner(49L, PropertyType.HOTEL);
        User customer = user(109L);
        Accommodation hotel = accommodation(49L, "LATA Hotel & Apartments", PropertyType.HOTEL, partner);
        Booking booking = booking(hotel);
        booking.setId(94L);
        booking.setUser(customer);
        booking.setBookingStatus(BookingStatus.PENDING_ADMIN_APPROVAL);
        booking.setPaymentStatus(PaymentStatus.PENDING_ADMIN_APPROVAL);
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.PENDING_ADMIN_APPROVAL);

        when(bookingRepository.findById(94L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByBooking(booking)).thenReturn(Optional.of(payment));

        Booking result = bookingService.approveBookingByAdmin(94L);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(result.getPartnerStatus()).isEqualTo(PartnerBookingStatus.PARTNER_CONFIRMED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
        verify(notificationService).createBookingConfirmed(customer, null, hotel);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole(User.Role.USER);
        return user;
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

    private static Room room(Long id, String roomCode, Accommodation accommodation) {
        Room room = new Room();
        room.setId(id);
        room.setRoomCode(roomCode);
        room.setRoomName(roomCode);
        room.setAccommodation(accommodation);
        room.setApprovalStatus(ApprovalStatus.APPROVED);
        room.setCapacity(2);
        room.setPricePerNight(new BigDecimal("1000000"));
        room.setAvailableQuantity(5);
        return room;
    }

    private static Booking booking(Accommodation accommodation) {
        Booking booking = new Booking();
        booking.setAccommodation(accommodation);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        return booking;
    }
}
