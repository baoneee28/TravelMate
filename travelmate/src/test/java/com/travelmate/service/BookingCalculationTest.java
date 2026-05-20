package com.travelmate.service;

import com.travelmate.entity.Room;
import com.travelmate.entity.enums.PaymentOption;
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

import static org.assertj.core.api.Assertions.assertThat;

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
                accommodationRepository, voucherService, notificationService
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

    // ─── Kịch bản thực tế (dữ liệu seed) ────────────────────────────────────

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
}
