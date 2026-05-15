package com.travelmate.entity;

import com.travelmate.entity.enums.PaymentMethod;
import com.travelmate.entity.enums.PaymentOption;
import com.travelmate.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment — Entity ánh xạ bảng `payments` trong MySQL.
 *
 * Lưu thông tin thanh toán cho mỗi booking.
 * Hiện tại chỉ dùng VNPay demo (mô phỏng, chưa tích hợp thật).
 *
 * Quan hệ:
 *   - 1 Payment thuộc 1 Booking (ManyToOne)
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Booking liên quan */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Phương thức thanh toán: VNPAY_DEMO, MOMO_DEMO */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.VNPAY_DEMO;

    /** Hình thức: FULL_PAYMENT hoặc DEPOSIT_30 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOption paymentOption;

    /** Số tiền thanh toán lần này */
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    /**
     * Mã giao dịch — sinh tự động dạng: TXN-<timestamp>.
     * Khi tích hợp VNPay thật sẽ lấy từ response VNPay.
     */
    @Column(length = 50)
    private String transactionCode;

    /** Trạng thái thanh toán — default PENDING_ADMIN_APPROVAL thay vì SUBMITTED cũ */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING_ADMIN_APPROVAL;

    /** Thời điểm thanh toán */
    @Column
    private LocalDateTime paidAt;

    /**
     * Thời điểm Admin duyệt thanh toán (APPROVED/DEPOSIT_FORFEITED).
     * Dùng để tính kỳ quyết toán chính xác hơn booking.createdAt.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Ghi chú (nếu có) */
    @Column(length = 500)
    private String note;
}
