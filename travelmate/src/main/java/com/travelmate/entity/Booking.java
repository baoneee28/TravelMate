package com.travelmate.entity;

import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PartnerBookingStatus;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.PaymentStatus;
import com.travelmate.entity.enums.RemainingPaymentStatus;
import com.travelmate.entity.enums.VoucherCostBearer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Booking — Entity ánh xạ bảng `bookings` trong MySQL.
 *
 * Luồng trạng thái (VNPAY):
 *   User tạo yêu cầu  → PENDING_PAYMENT  (phòng đã giữ tạm)
 *   VNPAY thành công  → PENDING_ADMIN_APPROVAL
 *   Admin xác nhận    → CONFIRMED
 *   Partner check-in  → CHECKED_IN
 *   Partner check-out → COMPLETED
 *
 * Booking code format: BK-<roomCode>-<sequence>
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã đặt phòng duy nhất: BK-<roomCode>-<sequence>.
     * Được sinh tự động trong BookingService.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String bookingCode;

    /** User đặt phòng (người đăng nhập) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Khách sạn được đặt */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;

    /** Phòng cụ thể được đặt */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    // ===== THÔNG TIN KHÁCH HÀNG =====

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(length = 20)
    private String customerPhone;

    @Column(length = 100)
    private String customerEmail;

    // ===== THÔNG TIN ĐẶT PHÒNG =====

    @Column(nullable = false)
    private LocalDate checkIn;

    @Column(nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private Integer adults = 1;

    @Column(nullable = false)
    private Integer children = 0;

    @Column(nullable = false)
    private Integer roomQuantity = 1;

    // ===== THÔNG TIN THANH TOÁN =====

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOption paymentOption;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal paidAmount;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal remainingAmount;

    // ===== TRẠNG THÁI =====

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BookingStatus bookingStatus = BookingStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING_PAYMENT;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_status", length = 40)
    private PartnerBookingStatus partnerStatus;

    // ===== VOUCHER / GIẢM GIÁ =====

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "discount_amount", precision = 15, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_cost_bearer", length = 20)
    private VoucherCostBearer voucherCostBearer;

    @Column(name = "total_before_discount", precision = 15, scale = 0)
    private BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

    // ===== NGUỒN BOOKING =====

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", length = 20)
    private BookingSource bookingSource = BookingSource.ONLINE;

    @Column(name = "block_reason", length = 300)
    private String blockReason;

    // ===== THANH TOÁN PHẦN CÒN LẠI (DEPOSIT_30) =====

    @Enumerated(EnumType.STRING)
    @Column(name = "remaining_payment_status", length = 30)
    private RemainingPaymentStatus remainingPaymentStatus = RemainingPaymentStatus.NOT_REQUIRED;

    @Column(name = "remaining_paid_at")
    private LocalDateTime remainingPaidAt;

    @Column(name = "remaining_payment_note", length = 300)
    private String remainingPaymentNote;

    // ===== VNPAY =====

    /**
     * Thời điểm booking PENDING_PAYMENT hết hạn (createdAt + 15 phút).
     * Scheduler sẽ hủy booking và trả lại quota nếu quá mốc này mà VNPAY chưa xác nhận.
     */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    /** Thời gian tạo booking */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
