package com.travelmate.util;

import com.travelmate.entity.enums.*;
import org.springframework.stereotype.Component;

/**
 * BusinessLabelUtil — Bộ nhãn nghiệp vụ chuẩn cho toàn hệ thống TravelMate.
 *
 * Dùng trong Thymeleaf: ${@businessLabelUtil.bookingStatusLabel(b.bookingStatus)}
 *
 * Nguyên tắc đặt tên:
 *   - "Ghi nhận thanh toán" dùng cho payment (không gọi là "Duyệt")
 *   - "Duyệt" chỉ dùng cho listing / room / nội dung
 *   - "Chờ ghi nhận chi trả" / "Đã ghi nhận chi trả" cho settlement
 *   - "Chặn bán trên TravelMate" cho MANUAL_BLOCK
 */
@Component("businessLabelUtil")
public class BusinessLabelUtil {

    // ─── BookingStatus ────────────────────────────────────────────────────────

    public String bookingStatusLabel(BookingStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PENDING_PAYMENT        -> "⏳ Chờ thanh toán VNPAY";
            case PENDING_ADMIN_APPROVAL -> "⚠ Cần Admin đối soát";
            case CONFIRMED              -> "✅ Booking đã được ghi nhận";
            case CHECKED_IN             -> "🏨 Khách đang lưu trú";
            case NO_SHOW                -> "🚫 Khách không đến";
            case CANCELLED              -> "❌ Đã hủy";
            case COMPLETED              -> "🏁 Đã hoàn tất";
        };
    }

    public String bookingStatusBadgeClass(BookingStatus status) {
        if (status == null) return "badge--muted";
        return switch (status) {
            case PENDING_PAYMENT        -> "badge--muted";
            case PENDING_ADMIN_APPROVAL -> "badge--warning";
            case CONFIRMED              -> "badge--primary";
            case CHECKED_IN             -> "badge--info";
            case COMPLETED              -> "badge--success";
            case NO_SHOW, CANCELLED     -> "badge--danger";
        };
    }

    // ─── PaymentStatus ────────────────────────────────────────────────────────

    public String paymentStatusLabel(PaymentStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PENDING_PAYMENT        -> "⏳ Đang chờ thanh toán VNPAY";
            case SUBMITTED              -> "⏳ Chờ đối soát thanh toán";   // legacy
            case PENDING_ADMIN_APPROVAL -> "⚠ Cần Admin đối soát";
            case APPROVED               -> "✅ TravelMate đã ghi nhận thanh toán";
            case FAILED                 -> "❌ Thanh toán VNPAY thất bại";
            case EXPIRED                -> "⌛ Quá hạn thanh toán";
            case REJECTED               -> "❌ Thanh toán bị từ chối";
            case CANCELLED              -> "🚫 Đã hủy";
            case DEPOSIT_FORFEITED      -> "💰 Giữ cọc 30% (hủy/no-show)";
            case REFUND_PENDING         -> "🔄 Chờ Admin xử lý hoàn tiền";
            case REFUNDED               -> "✅ Đã ghi nhận hoàn tiền";
            case NOT_REQUIRED           -> "—  Không qua TravelMate";
        };
    }

    public String paymentStatusLabelShort(PaymentStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PENDING_PAYMENT        -> "Chờ thanh toán VNPAY";
            case SUBMITTED              -> "Chờ đối soát";
            case PENDING_ADMIN_APPROVAL -> "Cần đối soát";
            case APPROVED               -> "Đã ghi nhận thanh toán";
            case FAILED                 -> "Thanh toán VNPAY thất bại";
            case EXPIRED                -> "Quá hạn thanh toán";
            case REJECTED               -> "Từ chối thanh toán";
            case CANCELLED              -> "Đã hủy";
            case DEPOSIT_FORFEITED      -> "Giữ cọc 30%";
            case REFUND_PENDING         -> "Chờ xử lý hoàn";
            case REFUNDED               -> "Đã ghi nhận hoàn";
            case NOT_REQUIRED           -> "Không áp dụng";
        };
    }

    // ─── PaymentOption ────────────────────────────────────────────────────────

    public String paymentOptionLabel(PaymentOption option) {
        if (option == null) return "—";
        return switch (option) {
            case FULL_PAYMENT -> "💳 Thanh toán 100% qua TravelMate";
            case DEPOSIT_30   -> "💰 Cọc 30% qua TravelMate — còn 70% tại cơ sở";
        };
    }

    public String paymentOptionShort(PaymentOption option) {
        if (option == null) return "—";
        return switch (option) {
            case FULL_PAYMENT -> "Thanh toán 100%";
            case DEPOSIT_30   -> "Cọc 30%";
        };
    }

    // ─── PartnerBookingStatus ─────────────────────────────────────────────────

    public String partnerStatusLabel(PartnerBookingStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PENDING_PARTNER_CONFIRMATION -> "⚠️ Cần kiểm tra trạng thái giữ phòng/căn";
            case PARTNER_CONFIRMED            -> "✅ TravelMate đã giữ phòng/căn";
            case PARTNER_COMPLETED            -> "🏁 Đã trả phòng / Hoàn tất";
            case PARTNER_CANCELLED            -> "❌ Không thể tiếp nhận khách";
        };
    }

    // ─── BookingSource ────────────────────────────────────────────────────────

    public String bookingSourceLabel(BookingSource source) {
        if (source == null) return "🌐 TravelMate Online";
        return switch (source) {
            case ONLINE       -> "🌐 TravelMate Online";
            case DIRECT       -> "🏃 Trực tiếp tại cơ sở";
            case MANUAL_BLOCK -> "🔒 Chặn bán trên TravelMate";
        };
    }

    // ─── SettlementStatus ────────────────────────────────────────────────────

    public String settlementStatusLabel(SettlementStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PENDING   -> "⏳ Chờ ghi nhận chi trả";
            case PAID      -> "✅ Đã ghi nhận chi trả";
            case CANCELLED -> "❌ Đã hủy";
        };
    }

    // ─── ApprovalStatus (Listing/Room) ────────────────────────────────────────

    public String approvalStatusLabel(ApprovalStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PENDING  -> "⏳ Chờ Admin duyệt listing";
            case APPROVED -> "✅ Đã duyệt — Đang mở bán";
            case REJECTED -> "❌ Bị từ chối — Liên hệ Admin";
        };
    }
}
