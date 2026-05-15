package com.travelmate.entity.enums;

/**
 * RemainingPaymentStatus — Trạng thái thanh toán phần còn lại (70%) tại nơi lưu trú.
 *
 * Chỉ áp dụng với booking DEPOSIT_30 (cọc 30%):
 *   NOT_REQUIRED       : Không áp dụng (booking FULL_PAYMENT hoặc DIRECT/BLOCK)
 *   UNPAID             : Chưa thu — khách chưa trả 70% tại cơ sở
 *   PAID_AT_PROPERTY   : Partner đã xác nhận thu đủ 70% tại cơ sở
 */
public enum RemainingPaymentStatus {
    NOT_REQUIRED,       // Không cần thu thêm (thanh toán 100% hoặc không phải online booking)
    UNPAID,             // Chưa thu phần còn lại tại cơ sở
    PAID_AT_PROPERTY    // Đã thu đủ tại cơ sở (partner xác nhận)
}
