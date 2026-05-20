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
 * Lưu toàn bộ thông tin giao dịch VNPAY cho mỗi booking.
 * Hỗ trợ cả Return URL lẫn IPN (server-to-server).
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

    /** Phương thức thanh toán */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentMethod paymentMethod = PaymentMethod.VNPAY;

    /** Hình thức: FULL_PAYMENT hoặc DEPOSIT_30 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOption paymentOption;

    /** Số tiền thanh toán lần này (VND) */
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    /** Mã giao dịch nội bộ TravelMate (TXN-<bookingId>-<timestamp>) */
    @Column(length = 50)
    private String transactionCode;

    /** Trạng thái thanh toán */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING_PAYMENT;

    /** Thời điểm user hoàn tất thanh toán trên VNPAY */
    @Column
    private LocalDateTime paidAt;

    /** Thời điểm Admin xác nhận thanh toán */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** Ghi chú nghiệp vụ */
    @Column(length = 500)
    private String note;

    // ===== VNPAY FIELDS =====

    /** Tên cổng thanh toán: "VNPAY" */
    @Column(length = 20)
    private String gateway;

    /**
     * Mã tham chiếu giao dịch TravelMate gửi lên VNPAY (vnp_TxnRef).
     * Duy nhất mỗi giao dịch. Dùng để tra cứu kết quả từ IPN/Return.
     */
    @Column(name = "vnp_txn_ref", length = 100, unique = true)
    private String vnpTxnRef;

    /** Mã giao dịch do VNPAY cấp (vnp_TransactionNo) */
    @Column(name = "vnp_transaction_no", length = 50)
    private String vnpTransactionNo;

    /** Mã ngân hàng user dùng để thanh toán (vnp_BankCode) */
    @Column(name = "vnp_bank_code", length = 20)
    private String vnpBankCode;

    /** Mã giao dịch phía ngân hàng (vnp_BankTranNo) */
    @Column(name = "vnp_bank_tran_no", length = 50)
    private String vnpBankTranNo;

    /** Loại thẻ: ATM hoặc CREDIT (vnp_CardType) */
    @Column(name = "vnp_card_type", length = 20)
    private String vnpCardType;

    /** Mã phản hồi từ VNPAY (vnp_ResponseCode): "00" = thành công */
    @Column(name = "vnp_response_code", length = 10)
    private String vnpResponseCode;

    /** Trạng thái giao dịch VNPAY (vnp_TransactionStatus) */
    @Column(name = "vnp_transaction_status", length = 10)
    private String vnpTransactionStatus;

    /** Thời gian thanh toán theo VNPAY (yyyyMMddHHmmss) */
    @Column(name = "vnp_pay_date", length = 20)
    private String vnpPayDate;

    /** Raw payload từ Return URL (để debug / audit) */
    @Column(name = "raw_return_payload", columnDefinition = "TEXT")
    private String rawReturnPayload;

    /** Raw payload từ IPN (để debug / audit) */
    @Column(name = "raw_ipn_payload", columnDefinition = "TEXT")
    private String rawIpnPayload;

    /** Thời điểm giao dịch hết hạn — VNPAY từ chối sau mốc này */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    /** Thời điểm VNPAY xác nhận giao dịch thành công (từ IPN/Return) */
    @Column(name = "confirmed_from_gateway_at")
    private LocalDateTime confirmedFromGatewayAt;
}
