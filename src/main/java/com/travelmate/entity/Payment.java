package com.travelmate.entity;

import com.travelmate.enums.PaymentMethod;
import com.travelmate.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity Payment - ánh xạ bảng "payments" trong MySQL.
 *
 * Quan hệ nghiệp vụ:
 *   1 Booking ↔ 1 Payment (quan hệ 1-1, UNIQUE booking_id)
 *
 * Lifecycle:
 *   Booking tạo → Payment(UNPAID, QR_INVOICE) tạo tự động
 *   User quét QR → nhấn "Đã chuyển khoản" → Payment giữ UNPAID (mock)
 *   Admin/Partner xác nhận → Payment(PAID)
 *   Booking hủy → Payment(REFUNDED) nếu đã PAID, hoặc FAILED nếu UNPAID
 *
 *   Với đồ án cơ sở: chỉ cần UNPAID → PAID (mock flow).
 *
 * Kế thừa BaseEntity: id, createdAt, updatedAt.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_status", columnList = "payment_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    /**
     * Booking tương ứng — quan hệ 1-1 (UNIQUE booking_id trong DB).
     *
     * @OneToOne(fetch = LAZY): chỉ load booking khi cần
     * cascade = NONE: không cascade delete (giữ payment record khi booking xóa)
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    /**
     * Số tiền cần thanh toán = booking.totalPrice.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Phương thức thanh toán.
     * Mặc định QR_INVOICE (mock flow QR bank transfer trong đồ án).
     * Mở rộng sau: VNPAY, MOMO, CASH.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.QR_INVOICE;

    /**
     * Trạng thái thanh toán.
     * Mặc định UNPAID — user cần quét QR và xác nhận.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    /**
     * Mã giao dịch (mock).
     * Format: QR{yyyyMMddHHmmss}TM{bookingId}
     * Ví dụ: QR20260501143022TM7
     * Null nếu chưa thanh toán.
     */
    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    /**
     * Thời điểm thanh toán thành công.
     * Null nếu chưa thanh toán.
     */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
