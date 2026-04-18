package com.travelmate.dto.response;

import com.travelmate.enums.PaymentMethod;
import com.travelmate.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO trả về thông tin payment từ API / template.
 *
 * Chứa đủ để hiển thị trên trang "Đặt phòng của tôi":
 * - Số tiền, trạng thái, phương thức, mã giao dịch, thời gian thanh toán
 * - Badge CSS class (unpaid/paid/refunded...) để tô màu trên UI
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /** ID payment */
    private Long id;

    /** ID booking tương ứng */
    private Long bookingId;

    /** Số tiền thanh toán */
    private BigDecimal amount;

    /** Số tiền đã format: "3.600.000 VNĐ" */
    private String amountFormatted;

    /** Phương thức thanh toán (enum) */
    private PaymentMethod paymentMethod;

    /** Nhãn phương thức tiếng Việt */
    private String paymentMethodLabel;

    /** Trạng thái thanh toán (enum) */
    private PaymentStatus paymentStatus;

    /** Nhãn trạng thái tiếng Việt: "Chưa thanh toán", "Đã thanh toán"... */
    private String paymentStatusLabel;

    /**
     * CSS class badge: "badge-unpaid", "badge-paid", "badge-refunded", "badge-failed"
     * Dùng trong template: th:class="${payment.statusCssClass}"
     */
    private String statusCssClass;

    /** Mã giao dịch mock (null nếu chưa thanh toán) */
    private String transactionCode;

    /** Thời điểm thanh toán thành công (null nếu chưa thanh toán) */
    private LocalDateTime paidAt;

    /** Thời gian tạo payment record */
    private LocalDateTime createdAt;
}
