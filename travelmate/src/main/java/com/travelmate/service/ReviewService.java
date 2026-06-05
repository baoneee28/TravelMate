package com.travelmate.service;

import com.travelmate.entity.*;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.ReviewRepository;
import com.travelmate.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ReviewService — Xử lý nghiệp vụ đánh giá.
 *
 * Chức năng:
 *   1. User tạo review sau khi booking COMPLETED
 *   2. Tự động cập nhật rating + reviewCount của accommodation
 *   3. Admin xem/ẩn/hiện review (soft-hide, không xóa vật lý)
 *   4. Kiểm tra booking đã review chưa
 */
@SuppressWarnings("null")
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final AccommodationRepository accommodationRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    public ReviewService(ReviewRepository reviewRepository,
                         BookingRepository bookingRepository,
                         AccommodationRepository accommodationRepository,
                         RoomRepository roomRepository,
                         NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.accommodationRepository = accommodationRepository;
        this.roomRepository = roomRepository;
        this.notificationService = notificationService;
    }

    // ─── User: Tạo review ────────────────────────────────────────────────────

    /**
     * User tạo review cho booking đã COMPLETED.
     *
     * Business rules:
     *   1. Booking phải tồn tại
     *   2. User phải là chủ booking
     *   3. BookingStatus phải là COMPLETED
     *   4. Booking chưa được review (1 booking = 1 review)
     *   5. Rating phải trong khoảng 1-10
     *   6. Comment không được rỗng
     *
     * Side effect: cập nhật accommodation.rating + reviewCount
     */
    @Transactional
    public Review createReview(User user, Long bookingId, int rating, String comment) {

        // Rule 1: Booking phải tồn tại
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng!"));

        // Rule 2: User phải là chủ booking
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền đánh giá đơn này!");
        }

        // Rule 2b: Chỉ booking ONLINE mới được đánh giá
        // DIRECT/MANUAL_BLOCK không phải booking user online — không có tài khoản user thực sự
        if (booking.getBookingSource() != null
                && booking.getBookingSource() != com.travelmate.entity.enums.BookingSource.ONLINE) {
            throw new RuntimeException("Chỉ có booking qua TravelMate Online mới có thể đánh giá. "
                    + "Booking trực tiếp hoặc chặn phòng không hỗ trợ đánh giá.");
        }

        // Rule 3: BookingStatus phải là COMPLETED
        if (booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException(
                "Chỉ có thể đánh giá đơn đã hoàn tất! " +
                "Trạng thái hiện tại: " + booking.getBookingStatus().name());
        }

        // Rule 4: Chưa review
        if (reviewRepository.existsByBooking(booking)) {
            throw new RuntimeException("Bạn đã đánh giá đơn này rồi!");
        }

        // Rule 5: Rating 1-10
        if (rating < 1 || rating > 10) {
            throw new RuntimeException("Điểm đánh giá phải từ 1 đến 10!");
        }

        // Rule 6: Comment không rỗng
        if (comment == null || comment.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập nội dung đánh giá!");
        }

        // Tạo review
        Review review = new Review();
        review.setUser(user);
        review.setAccommodation(booking.getAccommodation());
        review.setBooking(booking);
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setIsHidden(false);

        review = reviewRepository.save(review);

        // Cập nhật rating + reviewCount cho accommodation
        recalculateAccommodationRating(booking.getAccommodation());
        assessLowRatingForRoom(booking.getRoom());

        return review;
    }

    // ─── Xem reviews ─────────────────────────────────────────────────────────

    /**
     * Lấy danh sách review của 1 accommodation — hiển thị trên trang detail.
     * Chỉ trả về review chưa bị ẩn (isHidden = false).
     */
    public List<Review> getReviewsByAccommodation(Accommodation accommodation) {
        return reviewRepository.findByAccommodationOrderByCreatedAtDesc(accommodation)
                .stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsHidden()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách review của 1 accommodation theo ID.
     */
    public List<Review> getReviewsByAccommodationId(Long accommodationId) {
        Accommodation acc = accommodationRepository.findById(accommodationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nơi lưu trú!"));
        return getReviewsByAccommodation(acc);
    }

    /**
     * Lấy tất cả reviews — admin quản lý (bao gồm cả review đã ẩn).
     */
    public List<Review> getAllReviewsForAdmin() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Kiểm tra booking đã được review chưa.
     */
    public boolean hasReviewed(Long bookingId) {
        return reviewRepository.existsByBookingId(bookingId);
    }

    /**
     * Lấy set booking IDs đã review — dùng cho bulk check trên trang my-bookings.
     * Tránh N+1 query: check 1 lần thay vì gọi existsByBookingId cho từng booking.
     */
    public Set<Long> getReviewedBookingIds(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.COMPLETED)
                .filter(b -> reviewRepository.existsByBookingId(b.getId()))
                .map(Booking::getId)
                .collect(Collectors.toSet());
    }

    // ─── Partner: Xem review ─────────────────────────────────────────────────

    /**
     * Lấy tất cả reviews của các accommodation thuộc 1 partner.
     */
    public List<Review> getReviewsForPartner(User partner) {
        List<Accommodation> myAccommodations = accommodationRepository.findByOwner(partner);
        if (myAccommodations.isEmpty()) {
            return List.of();
        }
        return reviewRepository.findByAccommodationInOrderByCreatedAtDesc(myAccommodations)
                .stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsHidden()))
                .collect(Collectors.toList());
    }

    // ─── Admin: Ẩn/Hiện review ───────────────────────────────────────────────

    /**
     * Admin ẩn review (soft-hide). Review không bị xóa vật lý.
     * User sẽ không thấy review bị ẩn trên trang public.
     * Admin vẫn thấy trong danh sách với trạng thái "Đã ẩn".
     * Side effect: tính lại accommodation.rating (chỉ tính review chưa ẩn).
     */
    @Transactional
    public void hideReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại!"));
        review.setIsHidden(true);
        reviewRepository.save(review);
        recalculateAccommodationRating(review.getAccommodation());
    }

    /**
     * Admin hiện lại review đã ẩn.
     * Side effect: tính lại accommodation.rating.
     */
    @Transactional
    public void unhideReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Đánh giá không tồn tại!"));
        review.setIsHidden(false);
        reviewRepository.save(review);
        recalculateAccommodationRating(review.getAccommodation());
    }

    /**
     * @deprecated Dùng hideReview() thay thế. Giữ lại để không gây lỗi compile.
     */
    @Deprecated
    @Transactional
    public void deleteReviewByAdmin(Long reviewId) {
        hideReview(reviewId);
    }

    // ─── Thống kê ────────────────────────────────────────────────────────────

    /** Đếm tổng review toàn hệ thống */
    public long countAllReviews() {
        return reviewRepository.count();
    }

    /** Đếm review của 1 accommodation */
    public long countByAccommodation(Accommodation accommodation) {
        return reviewRepository.countByAccommodation(accommodation);
    }

    // ─── Private helper ──────────────────────────────────────────────────────

    /**
     * Tính lại average rating và reviewCount cho accommodation.
     * Chỉ tính review chưa bị ẩn (isHidden = false).
     *
     * Công thức: averageRating = sum(rating) / count, làm tròn 1 chữ số.
     *
     * Nếu không có review → giữ rating = 0.0, reviewCount = 0.
     */
    private void recalculateAccommodationRating(Accommodation accommodation) {
        List<Review> reviews = reviewRepository
                .findByAccommodationOrderByCreatedAtDesc(accommodation)
                .stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsHidden()))
                .collect(Collectors.toList());

        if (reviews.isEmpty()) {
            accommodation.setRating(0.0);
            accommodation.setReviewCount(0);
        } else {
            double averageRating = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            double ratingOn10 = Math.round(averageRating * 10.0) / 10.0;

            accommodation.setRating(ratingOn10);
            accommodation.setReviewCount(reviews.size());
        }

        accommodationRepository.save(accommodation);
    }

    private void assessLowRatingForRoom(Room room) {
        if (room == null || room.getId() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        MonthStats currentStats = resolveMonthStats(room, currentMonth);
        MonthStats previousStats = resolveMonthStats(room, previousMonth);

        if (currentStats.count == 0 || currentStats.average >= 2.0) {
            return;
        }

        User partner = room.getAccommodation() != null ? room.getAccommodation().getOwner() : null;
        if (partner == null) {
            return;
        }

        if (previousStats.count > 0 && previousStats.average < 2.0) {
            if (Boolean.TRUE.equals(room.getAvailableForBooking())) {
                room.setAvailableForBooking(false);
                roomRepository.save(room);
            }
            notificationService.createLowRatingSuspension(partner, room, currentMonth, currentStats.average);
        } else {
            notificationService.createLowRatingWarning(partner, room, currentMonth, currentStats.average);
        }
    }

    private MonthStats resolveMonthStats(Room room, YearMonth month) {
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        Double avg = reviewRepository.findAverageRatingByRoomAndCreatedAtBetweenAndIsHiddenFalse(room, from, to);
        long count = reviewRepository.countByBooking_RoomAndCreatedAtBetweenAndIsHiddenFalse(room, from, to);
        return new MonthStats(avg != null ? avg : 0.0, count);
    }

    private record MonthStats(double average, long count) {}
}
