package com.travelmate.controller.api;

import com.travelmate.dto.request.ReviewRequest;
import com.travelmate.dto.response.ApiResponse;
import com.travelmate.dto.response.ReviewResponse;
import com.travelmate.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ReviewApiController - REST API xử lý đánh giá.
 *
 * Base URL: /api/reviews
 *
 * Các endpoint:
 * - POST /api/reviews                          — Tạo review mới
 * - GET  /api/reviews/accommodation/{id}       — Lấy review của 1 accommodation
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewApiController {

    private final ReviewService reviewService;

    /**
     * TẠO REVIEW MỚI
     *
     * POST /api/reviews
     *
     * Request body (JSON):
     * {
     *   "bookingId": 1,
     *   "rating": 5,
     *   "comment": "Phòng đẹp, dịch vụ tốt!"
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {

        String userEmail = auth.getName();

        ReviewResponse review = reviewService.createReview(request, userEmail);

        return ResponseEntity.ok(
                ApiResponse.success("Đánh giá thành công! Cảm ơn bạn đã chia sẻ trải nghiệm.", review)
        );
    }

    /**
     * LẤY REVIEW THEO ACCOMMODATION
     *
     * GET /api/reviews/accommodation/{id}
     */
    @GetMapping("/accommodation/{accommodationId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByAccommodation(
            @PathVariable Long accommodationId) {

        List<ReviewResponse> reviews = reviewService.getReviewsByAccommodation(accommodationId);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách đánh giá thành công", reviews)
        );
    }
}
