package com.travelmate.controller.api;

import com.travelmate.dto.response.ApiResponse;
import com.travelmate.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AdminApiController — REST API cho các thao tác quản trị.
 *
 * CSRF được tắt cho /api/** nên fetch() từ browser không cần CSRF token.
 * Tất cả endpoint đều yêu cầu role ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminApiController {

    private final AdminService adminService;

    // ── Booking ───────────────────────────────────────────────────────────────

    @PutMapping("/bookings/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        adminService.updateBookingStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái booking"));
    }

    // ── Accommodation ─────────────────────────────────────────────────────────

    @PutMapping("/accommodations/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveAccommodation(@PathVariable Long id) {
        adminService.approveAccommodation(id);
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt nơi lưu trú"));
    }

    @PutMapping("/accommodations/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectAccommodation(@PathVariable Long id) {
        adminService.rejectAccommodation(id);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối nơi lưu trú"));
    }

    // ── User ─────────────────────────────────────────────────────────────────

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(@PathVariable Long id) {
        adminService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái tài khoản"));
    }

    // ── Review ────────────────────────────────────────────────────────────────

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        adminService.deleteReview(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa đánh giá"));
    }
}
