package com.travelmate.service;

import com.travelmate.dto.request.ReviewRequest;
import com.travelmate.dto.response.ReviewResponse;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến Review.
 *
 * 2 chức năng chính trong Bước 3:
 * 1. createReview            — Tạo đánh giá cho booking đã COMPLETED
 * 2. getReviewsByAccommodation — Lấy danh sách review của 1 accommodation
 */
public interface ReviewService {

    /**
     * Tạo đánh giá mới cho 1 booking.
     *
     * Điều kiện validate:
     * 1. Booking phải tồn tại và thuộc về user đang đăng nhập
     * 2. Booking phải có trạng thái COMPLETED
     * 3. Booking chưa được review trước đó (mỗi booking chỉ review 1 lần)
     *
     * @param request   DTO chứa bookingId, rating, comment
     * @param userEmail email user đang đăng nhập (từ Spring Security)
     * @return ReviewResponse thông tin review vừa tạo
     */
    ReviewResponse createReview(ReviewRequest request, String userEmail);

    /**
     * Lấy danh sách review của 1 accommodation.
     * Sắp xếp mới nhất trước.
     *
     * @param accommodationId ID accommodation
     * @return Danh sách ReviewResponse
     */
    List<ReviewResponse> getReviewsByAccommodation(Long accommodationId);
}
