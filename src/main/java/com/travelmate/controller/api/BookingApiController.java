package com.travelmate.controller.api;

import com.travelmate.dto.request.BookingRequest;
import com.travelmate.dto.response.ApiResponse;
import com.travelmate.dto.response.AvailabilityResponse;
import com.travelmate.dto.response.BookingResponse;
import com.travelmate.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * BookingApiController - REST API xử lý đặt phòng.
 *
 * Đây là controller kiểu REST (trả về JSON), khác với Page Controller (trả về HTML).
 * Được gọi từ JavaScript trong template (AJAX / fetch).
 *
 * Base URL: /api/bookings
 *
 * Các endpoint:
 * - GET    /api/bookings/availability  — Kiểm tra phòng còn trống (chống trùng ngày)
 * - POST   /api/bookings               — Tạo booking mới
 * - POST   /api/bookings/{id}/cancel   — Hủy booking
 *
 * BẢO MẬT:
 * - Tất cả endpoint yêu cầu đăng nhập (Spring Security kiểm tra)
 * - User chỉ thao tác được trên booking của chính mình (Service kiểm tra)
 * - CSRF được tắt cho /api/** trong SecurityConfig
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingApiController {

    private final BookingService bookingService;

    /**
     * KIỂM TRA TÌNH TRẠNG PHÒNG TRỐNG
     *
     * GET /api/bookings/availability?accommodationId=1&checkIn=2026-05-01&checkOut=2026-05-04
     *
     * Được gọi bởi JavaScript (AJAX) khi user chọn ngày trên form đặt phòng.
     * Trả về ngay lập tức để UI có thể cảnh báo trước khi user nhấn "Thanh toán".
     *
     * checkIn và checkOut là optional:
     * - Nếu không có → chỉ trả về danh sách ngày đã bận (dùng để tô màu date-picker)
     * - Nếu có → kiểm tra overlap + trả về available true/false
     *
     * Response (JSON):
     * {
     *   "success": true,
     *   "data": {
     *     "available": false,
     *     "conflictMessage": "Đã bận từ 01/05 → 04/05...",
     *     "bookedRanges": [
     *       { "checkIn": "2026-05-01", "checkOut": "2026-05-04" }
     *     ]
     *   }
     * }
     */
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkAvailability(
            @RequestParam Long accommodationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {

        AvailabilityResponse result = bookingService.checkAvailability(accommodationId, checkIn, checkOut);
        return ResponseEntity.ok(ApiResponse.success("OK", result));
    }

    /**
     * TẠO BOOKING MỚI
     *
     * POST /api/bookings
     *
     * Request body (JSON):
     * {
     *   "accommodationId": 1,
     *   "checkIn": "2026-05-01",
     *   "checkOut": "2026-05-04",
     *   "numAdults": 2,
     *   "numChildren": 1,
     *   "notes": "Cần phòng tầng cao"
     * }
     *
     * Response (JSON):
     * {
     *   "success": true,
     *   "message": "Đặt phòng thành công!",
     *   "data": { ...BookingResponse... }
     * }
     *
     * [CHỐNG TRÙNG NGÀY] Service sẽ kiểm tra overlap trước khi lưu.
     * Nếu trùng → trả về 400 Bad Request với message mô tả ngày bị bận.
     *
     * @param request  DTO chứa thông tin đặt phòng
     * @param auth     Spring Security authentication object — chứa email user đang đăng nhập
     * @return ApiResponse<BookingResponse> chứa thông tin booking vừa tạo
     *
     * @Valid: Spring tự kiểm tra các annotation validate (@NotNull, @Min...) trên BookingRequest.
     *         Nếu không hợp lệ → Spring trả về 400 Bad Request tự động.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication auth) {

        // auth.getName() trả về email của user đang đăng nhập
        // (vì trong CustomUserDetailsService, mình dùng email làm username)
        String userEmail = auth.getName();

        BookingResponse booking = bookingService.createBooking(request, userEmail);

        // Ưu tiên hiển thị bookingCode chuẩn (TM20260418-001), fallback về #TM{id} nếu null
        String displayCode = (booking.getBookingCode() != null)
                ? booking.getBookingCode()
                : "#TM" + booking.getId();

        return ResponseEntity.ok(
                ApiResponse.success("Đặt phòng thành công! Mã đặt phòng: " + displayCode, booking)
        );
    }

    /**
     * HỦY BOOKING
     *
     * POST /api/bookings/{id}/cancel
     *
     * Tại sao dùng POST thay vì DELETE?
     * - Hủy booking không phải xóa khỏi DB mà là đổi trạng thái → CANCELLED
     * - Dùng POST đơn giản hơn cho đồ án (không cần cấu hình thêm cho DELETE method)
     * - REST purist sẽ dùng PATCH, nhưng POST đủ tốt cho academic project
     *
     * Response (JSON):
     * {
     *   "success": true,
     *   "message": "Đã hủy đặt phòng thành công",
     *   "data": { ...BookingResponse... }
     * }
     *
     * @param id   ID booking cần hủy (lấy từ URL path)
     * @param auth Spring Security authentication — email user đang đăng nhập
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            Authentication auth) {

        String userEmail = auth.getName();

        BookingResponse booking = bookingService.cancelBooking(id, userEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Đã hủy đặt phòng thành công", booking)
        );
    }

    /**
     * XÁC NHẬN ĐÃ THANH TOÁN (MOCK FLOW)
     *
     * POST /api/bookings/{id}/confirm-payment
     *
     * Được gọi khi user bấm "Tôi đã chuyển khoản" trên modal QR.
     * Cập nhật:
     * - payment.paymentStatus: UNPAID → PAID
     * - payment.paidAt: now()
     * - booking.bookingStatus: PENDING → CONFIRMED
     *
     * Response (JSON):
     * {
     *   "success": true,
     *   "message": "Xác nhận thanh toán thành công! Booking đã được xác nhận.",
     *   "data": { ...BookingResponse với status CONFIRMED + payment PAID... }
     * }
     *
     * @param id   ID booking cần xác nhận thanh toán
     * @param auth Spring Security authentication
     */
    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmPayment(
            @PathVariable Long id,
            Authentication auth) {

        String userEmail = auth.getName();

        BookingResponse booking = bookingService.confirmPayment(id, userEmail);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "✅ Xác nhận thanh toán thành công! Mã đặt phòng " +
                        (booking.getBookingCode() != null ? booking.getBookingCode() : "#TM" + id) +
                        " đã được xác nhận.",
                        booking)
        );
    }
}

