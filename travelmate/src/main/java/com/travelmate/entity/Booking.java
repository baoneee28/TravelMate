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
 * Đại diện cho 1 đơn đặt phòng của user.
 *
 * Luồng trạng thái:
 *   1. User đặt phòng   → bookingStatus = PENDING_ADMIN_APPROVAL
 *   2. Admin duyệt      → bookingStatus = CONFIRMED
 *   3. User checkout     → bookingStatus = COMPLETED
 *   4. User/Admin hủy   → bookingStatus = CANCELLED
 *
 * Booking code format: BK-<roomCode>-<sequence>
 * VD: BK-R101-0001, BK-R202-0002
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
    @Column(nullable = false, unique = true, length = 30)
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

    /** Tên khách hàng (có thể khác tên user đăng nhập) */
    @Column(nullable = false, length = 100)
    private String customerName;

    /** SĐT khách hàng */
    @Column(length = 20)
    private String customerPhone;

    /** Email khách hàng */
    @Column(length = 100)
    private String customerEmail;

    // ===== THÔNG TIN ĐẶT PHÒNG =====

    /** Ngày nhận phòng */
    @Column(nullable = false)
    private LocalDate checkIn;

    /** Ngày trả phòng */
    @Column(nullable = false)
    private LocalDate checkOut;

    /** Số người lớn */
    @Column(nullable = false)
    private Integer adults = 1;

    /** Số trẻ em */
    @Column(nullable = false)
    private Integer children = 0;

    /** Số phòng đặt */
    @Column(nullable = false)
    private Integer roomQuantity = 1;

    // ===== THÔNG TIN THANH TOÁN =====

    /**
     * Tổng tiền booking = pricePerNight × numberOfNights × roomQuantity.
     */
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal totalAmount;

    /**
     * Hình thức thanh toán: FULL_PAYMENT (100%) hoặc DEPOSIT_30 (cọc 30%).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOption paymentOption;

    /**
     * Số tiền đã thanh toán.
     * - FULL_PAYMENT: paidAmount = totalAmount
     * - DEPOSIT_30:   paidAmount = totalAmount × 0.3
     */
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal paidAmount;

    /**
     * Số tiền còn lại cần thanh toán.
     * = totalAmount - paidAmount
     */
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal remainingAmount;

    // ===== TRẠNG THÁI =====

    /** Trạng thái đơn đặt phòng */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus bookingStatus = BookingStatus.PENDING_ADMIN_APPROVAL;

    /** Trạng thái thanh toán — default PENDING_ADMIN_APPROVAL thay vì SUBMITTED cũ */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING_ADMIN_APPROVAL;

    /**
     * Ghi chú nghiệp vụ (admin ghi khi xử lý no-show, check-in, v.v.)
     * VD: "Khách không đến, cọc 30% bị giữ lại."
     */
    @Column(length = 500)
    private String note;

    /**
     * Trạng thái xác nhận từ phía partner.
     * - null hoặc PENDING_PARTNER_CONFIRMATION: chờ partner xác nhận
     * - PARTNER_CONFIRMED: partner đã xác nhận giữ phòng
     * Được set khi admin duyệt booking → PENDING_PARTNER_CONFIRMATION.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "partner_status", length = 40)
    private PartnerBookingStatus partnerStatus;

    // ===== VOUCHER / GIẢM GIÁ (Hướng 2) =====

    /**
     * Mã voucher user đã dùng khi đặt phòng (nếu có).
     * null = không dùng voucher.
     */
    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    /**
     * Số tiền đã được giảm nhờ voucher.
     * = 0 nếu không dùng voucher.
     */
    @Column(name = "discount_amount", precision = 15, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * Ai chịu chi phí giảm giá:
     * - ADMIN  → không trừ vào settlement partner
     * - PARTNER → trừ vào settlement partner
     * null = không dùng voucher.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_cost_bearer", length = 20)
    private VoucherCostBearer voucherCostBearer;

    /**
     * Tổng tiền trước khi giảm giá voucher.
     * = pricePerNight × nights × roomQuantity
     * totalAmount = totalBeforeDiscount - discountAmount
     */
    @Column(name = "total_before_discount", precision = 15, scale = 0)
    private BigDecimal totalBeforeDiscount = BigDecimal.ZERO;

    // ===== NGUỒN BOOKING / LOẠI VẬN HÀNH =====

    /**
     * Nguồn gốc booking:
     * - ONLINE       : user đặt qua TravelMate (mặc định)
     * - DIRECT       : partner tạo cho khách trực tiếp tại cơ sở
     * - MANUAL_BLOCK : partner chặn phòng/bảo trì
     *
     * Chỉ ONLINE đi vào settlement và tính commission.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", length = 20)
    private BookingSource bookingSource = BookingSource.ONLINE;

    /**
     * Lý do chặn phòng — chỉ dùng với MANUAL_BLOCK.
     * VD: "Bảo trì điều hòa", "Giữ nội bộ", "Sửa chữa phòng"
     */
    @Column(name = "block_reason", length = 300)
    private String blockReason;

    // ===== THANH TOÁN PHẦN CÒN LẠI (DEPOSIT_30) =====

    /**
     * Trạng thái thu tiền còn lại (70%) tại nơi lưu trú.
     * Chỉ có nghĩa với DEPOSIT_30:
     *   NOT_REQUIRED     : FULL_PAYMENT hoặc booking DIRECT/BLOCK
     *   UNPAID           : chưa thu 70% tại cơ sở
     *   PAID_AT_PROPERTY : partner đã xác nhận thu đủ 70%
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "remaining_payment_status", length = 30)
    private RemainingPaymentStatus remainingPaymentStatus = RemainingPaymentStatus.NOT_REQUIRED;

    /**
     * Thời điểm partner xác nhận đã thu 70% tại cơ sở.
     */
    @Column(name = "remaining_paid_at")
    private LocalDateTime remainingPaidAt;

    /**
     * Ghi chú về khoản thu 70% — partner điền lúc xác nhận.
     * VD: "Khách thanh toán tiền mặt", "Chuyển khoản tại quầy"
     */
    @Column(name = "remaining_payment_note", length = 300)
    private String remainingPaymentNote;

    /** Thời gian tạo booking */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
