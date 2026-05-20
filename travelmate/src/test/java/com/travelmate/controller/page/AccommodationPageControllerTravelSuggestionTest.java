package com.travelmate.controller.page;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.TravelPost;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.TravelPostRepository;
import com.travelmate.service.AccommodationService;
import com.travelmate.service.AvailabilityService;
import com.travelmate.service.ReviewService;
import com.travelmate.service.TravelPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccommodationPageControllerTravelSuggestionTest {

    @Mock
    private AccommodationService accommodationService;

    @Mock
    private ReviewService reviewService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private TravelPostRepository travelPostRepository;

    private AccommodationPageController controller;

    @BeforeEach
    void setUp() {
        TravelPostService travelPostService = new TravelPostService(travelPostRepository);
        controller = new AccommodationPageController(
                accommodationService,
                reviewService,
                bookingRepository,
                availabilityService,
                travelPostService
        );
    }

    @Test
    void accommodationsPageAddsTravelPostsForDestinationKeyword() {
        List<Accommodation> hotels = List.of(accommodation(1L, "LATA Hotel & Apartments", "Đà Lạt", PropertyType.HOTEL));
        List<TravelPost> daLatPosts = List.of(post("8 trải nghiệm nên thử khi đến Đà Lạt", "Đà Lạt", "da-lat"));
        when(accommodationService.searchByType(PropertyType.HOTEL, "da lat")).thenReturn(hotels);
        when(availabilityService.checkAvailabilityForAccommodation(
                hotels.get(0), LocalDate.parse("2026-05-18"), LocalDate.parse("2026-05-19")))
                .thenReturn(List.of());
        when(travelPostRepository.findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of("da-lat"), TravelPost.Status.VISIBLE)).thenReturn(daLatPosts);

        Model model = new ExtendedModelMap();
        String view = controller.listHotels("HOTEL", "da lat", "2026-05-18", "2026-05-19",
                2, 0, 1, model);

        assertThat(view).isEqualTo("user/hotels");
        assertThat(model.getAttribute("travelPosts")).isEqualTo(daLatPosts);
        assertThat(model.getAttribute("travelDestSlug")).isEqualTo("da-lat");
        assertThat(model.getAttribute("travelDestLabel")).isEqualTo("Đà Lạt");
        assertThat(model.getAttribute("showTravelSuggestSection")).isEqualTo(true);
        assertThat(model.getAttribute("hasTravelPosts")).isEqualTo(true);
        assertThat(model.getAttribute("keyword")).isEqualTo("da lat");
        assertThat(model.getAttribute("checkIn")).isEqualTo("2026-05-18");
        assertThat(model.getAttribute("checkOut")).isEqualTo("2026-05-19");
        assertThat(model.getAttribute("checkInDisplay")).isEqualTo("18/05/2026");
        assertThat(model.getAttribute("checkOutDisplay")).isEqualTo("19/05/2026");
    }

    @Test
    void accommodationsPageFallsBackToAccommodationCityWhenKeywordIsHotelName() {
        List<Accommodation> hotels = List.of(accommodation(2L, "Tulip Hotel 2 Dalat", "Đà Lạt", PropertyType.HOTEL));
        List<TravelPost> daLatPosts = List.of(post("Cẩm nang du lịch Đà Lạt", "Đà Lạt", "da-lat"));
        when(accommodationService.searchByType(PropertyType.HOTEL, "Tulip Hotel")).thenReturn(hotels);
        when(travelPostRepository.findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of("tulip-hotel"), TravelPost.Status.VISIBLE)).thenReturn(List.of());
        when(travelPostRepository.findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of("da-lat"), TravelPost.Status.VISIBLE)).thenReturn(daLatPosts);

        Model model = new ExtendedModelMap();
        controller.listHotels("HOTEL", "Tulip Hotel", "", "", 2, 0, 1, model);

        assertThat(model.getAttribute("travelPosts")).isEqualTo(daLatPosts);
        assertThat(model.getAttribute("travelKeyword")).isEqualTo("Đà Lạt");
        assertThat(model.getAttribute("travelDestSlug")).isEqualTo("da-lat");
        assertThat(model.getAttribute("travelDestLabel")).isEqualTo("Đà Lạt");
        assertThat(model.getAttribute("showTravelSuggestSection")).isEqualTo(true);
    }

    @Test
    void accommodationsPageDoesNotShowDestinationSuggestionSectionWithoutKeyword() {
        when(accommodationService.searchByType(PropertyType.HOTEL, "")).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        controller.listHotels("HOTEL", "", "", "", 2, 0, 1, model);

        assertThat(model.getAttribute("travelPosts")).isEqualTo(List.of());
        assertThat(model.getAttribute("travelDestSlug")).isEqualTo("");
        assertThat(model.getAttribute("travelDestLabel")).isEqualTo("");
        assertThat(model.getAttribute("showTravelSuggestSection")).isEqualTo(false);
        assertThat(model.getAttribute("hasTravelPosts")).isEqualTo(false);
        verifyNoInteractions(travelPostRepository);
    }

    @Test
    void accommodationsPageReturnsEmptyTravelPostsWithoutCrashingWhenDestinationHasNoPosts() {
        List<Accommodation> hotels = List.of(accommodation(99L, "Binh Duong Demo Hotel", "Bình Dương", PropertyType.HOTEL));
        when(accommodationService.searchByType(PropertyType.HOTEL, "binh duong")).thenReturn(hotels);
        when(travelPostRepository.findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of("binh-duong"), TravelPost.Status.VISIBLE)).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.listHotels("HOTEL", "binh duong", "", "", 2, 0, 1, model);

        assertThat(view).isEqualTo("user/hotels");
        assertThat(model.getAttribute("travelPosts")).isEqualTo(List.of());
        assertThat(model.getAttribute("travelKeyword")).isEqualTo("Bình Dương");
        assertThat(model.getAttribute("travelDestLabel")).isEqualTo("Bình Dương");
        assertThat(model.getAttribute("showTravelSuggestSection")).isEqualTo(true);
        assertThat(model.getAttribute("hasTravelPosts")).isEqualTo(false);
        verify(travelPostRepository).findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of("binh-duong"), TravelPost.Status.VISIBLE);
    }

    @ParameterizedTest
    @CsvSource({
            "VILLA,nha trang,Nha Trang,nha-trang",
            "HOMESTAY,hoi an,Hội An,hoi-an",
            "RESORT,phu quoc,Phú Quốc,phu-quoc"
    })
    void travelSuggestionsDependOnDestinationNotAccommodationType(
            String type, String keyword, String destinationLabel, String destinationSlug) {
        PropertyType propertyType = PropertyType.valueOf(type);
        List<Accommodation> accommodations = List.of(accommodation(10L, "Demo " + type, destinationLabel, propertyType));
        List<TravelPost> posts = List.of(post("Gợi ý " + destinationLabel, destinationLabel, destinationSlug));
        when(accommodationService.searchByType(propertyType, keyword)).thenReturn(accommodations);
        when(travelPostRepository.findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of(destinationSlug), TravelPost.Status.VISIBLE)).thenReturn(posts);

        Model model = new ExtendedModelMap();
        controller.listHotels(type, keyword, "", "", 2, 0, 1, model);

        assertThat(model.getAttribute("currentType")).isEqualTo(type);
        assertThat(model.getAttribute("travelPosts")).isEqualTo(posts);
        assertThat(model.getAttribute("travelDestLabel")).isEqualTo(destinationLabel);
        assertThat(model.getAttribute("travelDestSlug")).isEqualTo(destinationSlug);
    }

    private static Accommodation accommodation(Long id, String name, String city, PropertyType propertyType) {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(id);
        accommodation.setName(name);
        accommodation.setCity(city);
        accommodation.setPropertyType(propertyType);
        accommodation.setApprovalStatus(ApprovalStatus.APPROVED);
        accommodation.setStarRating(4);
        accommodation.setRating(8.8);
        accommodation.setReviewCount(100);
        accommodation.setThumbnailUrl("/assets/images/resort.jpg");
        return accommodation;
    }

    private static TravelPost post(String title, String destination, String destinationSlug) {
        TravelPost post = new TravelPost();
        post.setTitle(title);
        post.setDestination(destination);
        post.setDestinationSlug(destinationSlug);
        post.setSummary("Tóm tắt demo cho bài viết du lịch.");
        post.setSourceName("Vietnam.travel");
        post.setSourceUrl("https://vietnam.travel/places-to-go/central-vietnam/dalat");
        post.setCategory(TravelPost.Category.GUIDE);
        post.setStatus(TravelPost.Status.VISIBLE);
        return post;
    }
}
