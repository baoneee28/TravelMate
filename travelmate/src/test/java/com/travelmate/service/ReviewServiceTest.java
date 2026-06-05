package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Booking;
import com.travelmate.entity.Review;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.BookingSource;
import com.travelmate.entity.enums.BookingStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.ReviewRepository;
import com.travelmate.repository.RoomRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService — Review theo thang điểm 10")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private AccommodationRepository accommodationRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private NotificationService notificationService;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, bookingRepository,
                accommodationRepository, roomRepository, notificationService);
    }

    @Test
    @DisplayName("User tao review thang 10 cho booking completed online va cap nhat diem hien thi")
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
                    review.setRating(8);
                    review.setIsHidden(false);
                    return List.of(review);
                });

        Review review = reviewService.createReview(user, 30L, 8, "Phòng sạch, vị trí thuận tiện");

        assertThat(review.getUser()).isEqualTo(user);
        assertThat(review.getBooking()).isEqualTo(booking);
        assertThat(review.getComment()).isEqualTo("Phòng sạch, vị trí thuận tiện");
        assertThat(accommodation.getRating()).isEqualTo(8.0);
        assertThat(accommodation.getReviewCount()).isEqualTo(1);
        verify(accommodationRepository).save(accommodation);
    }

    @Test
    @DisplayName("Diem review ngoai thang 1-10 bi tu choi")
    void ratingOutsideTenPointScaleIsRejected() {
        User user = user(10L);
        Accommodation accommodation = accommodation(20L);
        Booking booking = booking(30L, user, accommodation, BookingStatus.COMPLETED, BookingSource.ONLINE);
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking(booking)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.createReview(user, 30L, 11, "Điểm không hợp lệ"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("1 đến 10");
        verify(reviewRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Booking truc tiep hoac manual block khong duoc review nhu booking online")
    void directBookingCannotBeReviewedByUserFlow() {
        User user = user(10L);
        Accommodation accommodation = accommodation(20L);
        Booking booking = booking(30L, user, accommodation, BookingStatus.COMPLETED, BookingSource.DIRECT);
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> reviewService.createReview(user, 30L, 10, "Tốt"))
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
        review.setRating(10);
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

    @Test
    @DisplayName("Room dưới 2/10 hai tháng liên tiếp sẽ bị tạm ngừng mở bán")
    void lowRatingsForTwoConsecutiveMonthsSuspendRoomBooking() {
        User partner = user(40L);
        Accommodation accommodation = accommodation(20L);
        accommodation.setOwner(partner);
        Room room = new Room();
        room.setId(50L);
        room.setRoomName("Deluxe 01");
        room.setAccommodation(accommodation);
        room.setAvailableForBooking(true);

        User user = user(10L);
        Booking booking = booking(30L, user, accommodation, BookingStatus.COMPLETED, BookingSource.ONLINE);
        booking.setRoom(room);
        when(bookingRepository.findById(30L)).thenReturn(Optional.of(booking));
        when(reviewRepository.existsByBooking(booking)).thenReturn(false);
        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Review existingReview = new Review();
        existingReview.setRating(1);
        existingReview.setIsHidden(false);
        when(reviewRepository.findByAccommodationOrderByCreatedAtDesc(accommodation))
                .thenReturn(List.of(existingReview));
        when(reviewRepository.findAverageRatingByRoomAndCreatedAtBetweenAndIsHiddenFalse(eq(room), any(), any()))
                .thenReturn(1.4, 1.8);
        when(reviewRepository.countByBooking_RoomAndCreatedAtBetweenAndIsHiddenFalse(eq(room), any(), any()))
                .thenReturn(1L, 1L);

        reviewService.createReview(user, 30L, 1, "Trải nghiệm chưa tốt");

        assertThat(room.getAvailableForBooking()).isFalse();
        verify(roomRepository).save(room);
        verify(notificationService).createLowRatingSuspension(eq(partner), eq(room), any(), org.mockito.ArgumentMatchers.anyDouble());
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
