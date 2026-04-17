package com.travelmate.service.impl;

import com.travelmate.dto.request.ReviewRequest;
import com.travelmate.dto.response.ReviewResponse;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Review;
import com.travelmate.entity.User;
import com.travelmate.enums.BookingStatus;
import com.travelmate.exception.BadRequestException;
import com.travelmate.exception.ResourceNotFoundException;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.ReviewRepository;
import com.travelmate.repository.UserRepository;
import com.travelmate.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation của ReviewService — xử lý toàn bộ nghiệp vụ đánh giá.
 *
 * Quy tắc quan trọng:
 * - Chỉ booking COMPLETED mới được review
 * - Mỗi booking chỉ review 1 lần (unique constraint trong DB)
 * - User chỉ review được booking của chính mình (bảo mật)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    // ========================================================================
    // 1. TẠO REVIEW MỚI
    // ========================================================================

    /**
     * Tạo đánh giá mới cho booking đã hoàn thành.
     *
     * Luồng xử lý:
     * 1. Tìm user đang đăng nhập
     * 2. Tìm booking (kèm accommodation) — kiểm tra thuộc về user
     * 3. Validate: booking phải COMPLETED
     * 4. Validate: booking chưa được review
     * 5. Tạo Review entity, save vào DB
     * 6. Trả về ReviewResponse
     */
    @Override
    public ReviewResponse createReview(ReviewRequest request, String userEmail) {

        // Bước 1: Tìm user đang đăng nhập
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài khoản: " + userEmail));

        // Bước 2: Tìm booking — BẢO MẬT: phải thuộc về user đang đăng nhập
        Booking booking = bookingRepository.findByIdAndUserId(request.getBookingId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đặt phòng hoặc bạn không có quyền đánh giá"));

        // Bước 3: Validate — booking phải COMPLETED
        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException(
                    "Chỉ có thể đánh giá đặt phòng đã hoàn thành. " +
                    "Trạng thái hiện tại: " + getStatusLabel(booking.getBookingStatus()));
        }

        // Bước 4: Validate — mỗi booking chỉ review 1 lần
        if (reviewRepository.existsByBookingId(request.getBookingId())) {
            throw new BadRequestException("Bạn đã đánh giá đặt phòng này rồi");
        }

        // Bước 5: Tạo Review entity
        Review review = Review.builder()
                .user(user)
                .accommodation(booking.getAccommodation())
                .booking(booking)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review savedReview = reviewRepository.save(review);

        // Bước 6: Trả về response
        return toResponse(savedReview);
    }

    // ========================================================================
    // 2. LẤY REVIEW THEO ACCOMMODATION
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByAccommodation(Long accommodationId) {

        List<Review> reviews = reviewRepository.findByAccommodationIdWithUser(accommodationId);

        return reviews.stream()
                .map(this::toResponse)
                .toList();
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    /**
     * Chuyển đổi entity Review → DTO ReviewResponse.
     */
    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .bookingId(review.getBooking().getId())
                .accommodationId(review.getAccommodation().getId())
                .accommodationName(review.getAccommodation().getName())
                .userName(review.getUser().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Nhãn trạng thái booking tiếng Việt (dùng cho thông báo lỗi).
     */
    private String getStatusLabel(BookingStatus status) {
        return switch (status) {
            case PENDING   -> "Chờ xác nhận";
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
            case COMPLETED -> "Hoàn thành";
        };
    }
}
