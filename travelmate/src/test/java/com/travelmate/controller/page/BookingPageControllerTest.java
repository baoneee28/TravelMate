package com.travelmate.controller.page;

import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import com.travelmate.security.CustomUserDetails;
import com.travelmate.service.AccommodationService;
import com.travelmate.service.AvailabilityService;
import com.travelmate.service.BookingService;
import com.travelmate.service.ReviewService;
import com.travelmate.service.RoomImageService;
import com.travelmate.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BookingPageControllerTest {

    @Mock private AccommodationService accommodationService;
    @Mock private BookingService bookingService;
    @Mock private ReviewService reviewService;
    @Mock private UserRepository userRepository;
    @Mock private AvailabilityService availabilityService;
    @Mock private RoomImageService roomImageService;
    @Mock private VoucherService voucherService;

    @InjectMocks private BookingPageController bookingPageController;

    @Test
    void confirmBookingGetFallbackWithoutRoomIdRedirectsToAccommodationList() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = bookingPageController.confirmBookingGetFallback(
                null, "", "", 2, 0, 1, "", userPrincipal(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/accommodations");
        assertThat(redirectAttributes.getFlashAttributes())
                .containsKey("errorMessage");
    }

    @Test
    void confirmBookingGetFallbackWithRoomIdReturnsToBookingForm() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = bookingPageController.confirmBookingGetFallback(
                12L, "2026-06-10", "2026-06-12", 2, 1, 1, "SUMMER10", userPrincipal(), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/booking?roomId=12&adults=2&children=1&rooms=1&checkIn=2026-06-10&checkOut=2026-06-12&voucherCode=SUMMER10");
        assertThat(redirectAttributes.getFlashAttributes())
                .containsKey("errorMessage");
    }

    private CustomUserDetails userPrincipal() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@travelmate.vn");
        user.setName("Nguyễn Văn An");
        user.setPhone("0912345678");
        user.setPassword("encoded");
        user.setRole(User.Role.USER);
        user.setStatus("ACTIVE");
        return new CustomUserDetails(user);
    }
}
