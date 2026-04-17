package com.travelmate.controller.api;

import com.travelmate.dto.request.BookingRequest;
import com.travelmate.dto.response.ApiResponse;
import com.travelmate.dto.response.BookingResponse;
import com.travelmate.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * BookingApiController - REST API xử lý đặt phòng.
 *
 * Đây là controller kiểu REST (trả về JSON), khác với Page Controller (trả về HTML).
 * Được gọi từ JavaScript trong template (AJAX / fetch).
 *
 * Base URL: /api/bookings
 *
 * Các endpoint:
 * - POST   /api/bookings        — Tạo booking mới
 * - POST   /api/bookings/{id}/cancel — Hủy booking
 *
 * BẢO MẬT:
 * - Tất cả endpoint yêu cầu đăng nhập (Spring Security kiểm tra)
 * - User chỉ thao tác được trên booking của chính mình (Service kiểm tra)
 * - CSRF được tắt cho /api/** trong SecurityConfig
 *
 * Annotation giải thích:
 * - @RestController: mỗi method trả về JSON (không phải view name)
 * - @RequestMapping("/api/bookings"): base URL cho tất cả endpoint
 * - @RequiredArgsConstructor: Lombok inject BookingService qua constructor
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingApiController {

    private final BookingService bookingService;

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

        return ResponseEntity.ok(
                ApiResponse.success("Đặt phòng thành công! Mã đặt phòng: #TM" + booking.getId(), booking)
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
}
