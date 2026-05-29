package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Review;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService — Đánh giá từ khách hàng thang 1-10")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private AccommodationRepository accommodationRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, bookingRepository, accommodationRepository);
    }

    @Test
    @DisplayName("User tao review cho booking completed online va cap nhat rating thang 10")
    void completedOnlineBookingCanBeReviewedAndRecalculatesRating() {
        User user = user(10L);
        Accommodation accommodation = accommodation(20L);
        Booking booking = booking(30L, user, accommodation, BookingStatus.COMPLETED, BookingSource.ONLINE);
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking(booking)).thenReturn(false);
        when(reviewRepository.save(org.mockito.ArgumentMatchers.any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewRepository.findByAccommodationOrderByCreatedAtDesc(accommodation))
                .thenAnswer(invocation -> {
                    Review review = new Review();
                    review.setRating(4);
                    review.setIsHidden(false);
                    return List.of(review);
                });

        Review review = reviewService.createReview(user, 30L, 4, "Phòng sạch, vị trí thuận tiện");

        assertThat(review.getUser()).isEqualTo(user);
        assertThat(review.getBooking()).isEqualTo(booking);
        assertThat(review.getComment()).isEqualTo("Phòng sạch, vị trí thuận tiện");
        assertThat(accommodation.getRating()).isEqualTo(8.0);
        assertThat(accommodation.getReviewCount()).isEqualTo(1);
        verify(accommodationRepository).save(accommodation);
    }

    @Test
    @DisplayName("Booking truc tiep hoac manual block khong duoc review nhu booking online")
    void directBookingCannotBeReviewedByUserFlow() {
        User user = user(10L);
        Accommodation accommodation = accommodation(20L);
        Booking booking = booking(30L, user, accommodation, BookingStatus.COMPLETED, BookingSource.DIRECT);
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> reviewService.createReview(user, 30L, 5, "Tốt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("TravelMate Online");
        verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Admin an review thi reviewCount va rating duoc tinh lai bo qua review an")
    void hidingReviewRecalculatesAccommodationRatingWithoutHiddenReviews() {
        Accommodation accommodation = accommodation(20L);
        Review review = new Review();
        review.setId(99L);
        review.setAccommodation(accommodation);
        review.setRating(5);
        review.setIsHidden(false);
        when(reviewRepository.findById(99L)).thenReturn(Optional.of(review));
        when(reviewRepository.findByAccommodationOrderByCreatedAtDesc(accommodation)).thenReturn(List.of(review));

        reviewService.hideReview(99L);

        assertThat(review.getIsHidden()).isTrue();
        assertThat(accommodation.getRating()).isEqualTo(0.0);
        assertThat(accommodation.getReviewCount()).isZero();
        verify(reviewRepository).save(review);
        verify(accommodationRepository).save(accommodation);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@travelmate.vn");
        return user;
    }

    private static Accommodation accommodation(Long id) {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(id);
        accommodation.setName("TravelMate Hotel");
        accommodation.setCity("Đà Lạt");
        accommodation.setPropertyType(PropertyType.HOTEL);
        return accommodation;
    }

    private static Booking booking(Long id, User user, Accommodation accommodation,
                                   BookingStatus status, BookingSource source) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setAccommodation(accommodation);
        booking.setBookingStatus(status);
        booking.setBookingSource(source);
        return booking;
    }
}
