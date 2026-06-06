package com.travelmate.service;

import com.travelmate.dto.RoomAvailabilityDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.RoomImage;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.entity.enums.VoucherScope;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.TravelPostRepository;
import com.travelmate.repository.UserRepository;
import com.travelmate.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private TravelPostRepository travelPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private AvailabilityService availabilityService;

    @Mock
    private RoomImageService roomImageService;

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                accommodationRepository,
                travelPostRepository,
                userRepository,
                bookingRepository,
                voucherRepository,
                availabilityService,
                roomImageService
        );
        lenient().when(travelPostRepository.findTop3ByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                anyCollection(), eq(com.travelmate.entity.TravelPost.Status.VISIBLE)))
                .thenReturn(List.of());
        lenient().when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of());
        lenient().when(roomImageService.getImages(any(Room.class))).thenReturn(List.of());
    }

    @Test
    void greetingUnderstandsShortGreetingAndCommonTypo() {
        assertThat(chatbotService.processMessage("helo", null).intent()).isEqualTo("GREETING");
        assertThat(chatbotService.processMessage("hi", null).intent()).isEqualTo("GREETING");
        assertThat(chatbotService.processMessage("halo", null).intent()).isEqualTo("GREETING");
    }

    @Test
    void blankMessageGreetsSignedInUserByLastName() {
        User user = new User();
        user.setName("Nguyen Minh An");
        when(userRepository.findByEmail("an@travelmate.vn")).thenReturn(Optional.of(user));

        ChatbotService.ChatbotResponse response = chatbotService.processMessage(" ", "an@travelmate.vn");

        assertThat(response.intent()).isEqualTo("GREETING");
        assertThat(response.reply()).contains("An", "TravelBot");
        assertThat(response.quickReplies()).contains("Đặt phòng của tôi");
    }

    @Test
    void budgetPlanUnderstandsMoneyAndDestinationBeforeFallback() {
        Accommodation homestay = accommodation(1L, "Sapa Valley Homestay", "Sa Pa", PropertyType.HOMESTAY, 380_000D);
        Accommodation hotel = accommodation(2L, "Sapa Cloud Valley Hotel", "Sa Pa", PropertyType.HOTEL, 720_000D);
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(homestay, hotel));
        when(availabilityService.checkAvailabilityForAccommodation(eq(homestay), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(5L, homestay, "Phòng Tiêu Chuẩn", 380_000, 4)));
        when(availabilityService.checkAvailabilityForAccommodation(eq(hotel), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(6L, hotel, "Phòng Deluxe", 720_000, 3)));

        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Tôi có khoảng 3 triệu và muốn đi Sa Pa", null);

        assertThat(response.intent()).isEqualTo("BUDGET_TRAVEL_PLAN");
        assertThat(response.reply())
                .contains("3.000.000đ")
                .contains("Sa Pa")
                .contains("TravelMate tạm tính")
                .contains("1 đêm")
                .contains("Sapa Valley Homestay");
    }

    @Test
    void budgetPlanSuggestsBeachDestinationsVisibly() {
        Accommodation nhaTrang = accommodation(3L, "InterContinental Nha Trang", "Nha Trang", PropertyType.HOTEL, 1_850_000D);
        Accommodation daNang = accommodation(4L, "Novotel Đà Nẵng Premier", "Đà Nẵng", PropertyType.HOTEL, 1_100_000D);
        Accommodation phuQuoc = accommodation(5L, "Sunset Pearl Resort Phú Quốc", "Phú Quốc", PropertyType.RESORT, 2_100_000D);
        Accommodation vungTau = accommodation(6L, "Pullman Vũng Tàu", "Vũng Tàu", PropertyType.HOTEL, 1_650_000D);
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(nhaTrang, daNang, phuQuoc, vungTau));
        when(availabilityService.checkAvailabilityForAccommodation(eq(nhaTrang), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(7L, nhaTrang, "Deluxe Biển", 1_850_000, 2)));
        when(availabilityService.checkAvailabilityForAccommodation(eq(daNang), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(8L, daNang, "Superior", 1_100_000, 5)));
        when(availabilityService.checkAvailabilityForAccommodation(eq(phuQuoc), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(9L, phuQuoc, "Bungalow", 2_100_000, 2)));
        when(availabilityService.checkAvailabilityForAccommodation(eq(vungTau), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(10L, vungTau, "Pool View", 1_650_000, 2)));

        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("4 triệu muốn đi biển", null);

        assertThat(response.intent()).isEqualTo("BUDGET_TRAVEL_PLAN");
        assertThat(response.reply())
                .contains("du lịch biển")
                .contains("Nha Trang")
                .contains("Đà Nẵng")
                .contains("Xem nơi lưu trú")
                .contains("Xem gợi ý du lịch")
                .contains("Xem voucher");
    }

    @Test
    void roomPriceSearchChecksDatesAndLinksDirectlyToAvailableRoom() {
        Accommodation homestay = accommodation(1L, "LATA Hotel & Apartments", "Đà Lạt", PropertyType.HOMESTAY, 650_000D);
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(homestay));
        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(PropertyType.HOMESTAY, ApprovalStatus.APPROVED))
                .thenReturn(List.of(homestay));
        when(availabilityService.checkAvailabilityForAccommodation(
                homestay, LocalDate.of(2026, 5, 28), LocalDate.of(2026, 5, 31)))
                .thenReturn(List.of(availableRoom(5L, homestay, "Phòng Tiêu Chuẩn Giường King", 650_000, 3)));

        ChatbotService.ChatbotResponse response = chatbotService.processMessage(
                "Homestay Đà Lạt dưới 2 triệu từ 28/05/2026 đến 31/05/2026, 2 người", null);

        assertThat(response.intent()).isEqualTo("BUDGET_TRAVEL_PLAN");
        assertThat(response.reply())
                .contains("Mức giá phòng/đêm bạn yêu cầu")
                .contains("Phòng Tiêu Chuẩn Giường King")
                .contains("còn 3 phòng")
                .contains("checkIn=2026-05-28")
                .contains("#room-5")
                .contains("Xem phòng này");
    }

    @Test
    void roomSearchFiltersRoomsThatCannotHostRequestedGuests() {
        Accommodation small = accommodation(11L, "Nhà Nhỏ Đà Lạt", "Đà Lạt", PropertyType.HOMESTAY, 650_000D);
        Accommodation suitable = accommodation(12L, "Pine Family Homestay", "Đà Lạt", PropertyType.HOMESTAY, 1_400_000D);
        addRoomCapacity(small, 111L, 2);
        addRoomCapacity(suitable, 112L, 4);
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(small, suitable));
        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(PropertyType.HOMESTAY, ApprovalStatus.APPROVED))
                .thenReturn(List.of(small, suitable));
        when(availabilityService.checkAvailabilityForAccommodation(eq(small), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(111L, small, "Phòng đôi nhỏ", 650_000, 2)));
        when(availabilityService.checkAvailabilityForAccommodation(eq(suitable), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(112L, suitable, "Phòng gia đình", 1_400_000, 1)));

        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Homestay Đà Lạt dưới 2 triệu cho 4 người", null);

        assertThat(response.intent()).isEqualTo("BUDGET_TRAVEL_PLAN");
        assertThat(response.reply())
                .contains("Pine Family Homestay", "#room-112")
                .doesNotContain("Nhà Nhỏ Đà Lạt", "#room-111");
    }

    @Test
    void villaSearchReturnsDatabaseRoomForRequestedDestinationAndCapacity() {
        Accommodation villa = accommodation(20L, "Biển Xanh Villa", "Nha Trang", PropertyType.VILLA, 3_200_000D);
        addRoomCapacity(villa, 201L, 6);
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(villa));
        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(PropertyType.VILLA, ApprovalStatus.APPROVED))
                .thenReturn(List.of(villa));
        when(availabilityService.checkAvailabilityForAccommodation(eq(villa), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(201L, villa, "Villa nguyên căn", 3_200_000, 1)));

        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Tìm villa Nha Trang cho 6 người", null);

        assertThat(response.reply())
                .contains("Biển Xanh Villa", "Villa nguyên căn", "#room-201")
                .contains("6 khách");
    }

    @Test
    void unavailableRoomIsNotRecommendedForSelectedDates() {
        Accommodation hotel = accommodation(30L, "River Hotel Đà Nẵng", "Đà Nẵng", PropertyType.HOTEL, 800_000D);
        addRoomCapacity(hotel, 301L, 2);
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(hotel));
        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(PropertyType.HOTEL, ApprovalStatus.APPROVED))
                .thenReturn(List.of(hotel));
        when(availabilityService.checkAvailabilityForAccommodation(eq(hotel), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(301L, hotel, "Superior", 800_000, 0)));

        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Khách sạn Đà Nẵng 2 người 1 phòng", null);

        assertThat(response.reply())
                .contains("Hiện chưa còn")
                .doesNotContain("#room-301");
    }

    @Test
    void benTreHotelBookingQuestionReturnsDirectRoomChoice() {
        Accommodation hotel = accommodation(40L, "Dz Hoàng", "Bến Tre", PropertyType.HOTEL, 200_000D);
        Room room = addRoomCapacity(hotel, 401L, 3);
        RoomImage roomImage = new RoomImage();
        roomImage.setImageUrl("/uploads/rooms/ben-tre-room-1.jpg");
        when(roomImageService.getImages(room)).thenReturn(List.of(roomImage));
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(hotel));
        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(PropertyType.HOTEL, ApprovalStatus.APPROVED))
                .thenReturn(List.of(hotel));
        when(availabilityService.checkAvailabilityForAccommodation(eq(hotel), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(401L, hotel, "phòng vip", 200_000, 5)));

        ChatbotService.ChatbotResponse suggestion = chatbotService.processMessage(
                "Hãy gợi ý cho tôi khách sạn ở Bến Tre", null);
        ChatbotService.ChatbotResponse booking = chatbotService.processMessage(
                "tôi muốn đặt khách sạn tại bến tre", null);
        ChatbotService.ChatbotResponse followUp = chatbotService.processMessage(
                "nhưng mà tôi muốn đặt", null, "Bến Tre");

        assertThat(suggestion.intent()).isEqualTo("FIND_ACCOMMODATION");
        assertThat(suggestion.reply())
                .contains("Bến Tre", "Dz Hoàng", "phòng vip", "200.000đ", "#room-401", "Xem phòng này")
                .contains("/uploads/rooms/ben-tre-room-1.jpg")
                .doesNotContain("/assets/images/homestay-sapa.jpg")
                .doesNotContain("Bạn muốn tìm");
        assertThat(suggestion.quickReplies()).contains("Đặt phòng Bến Tre");
        assertThat(booking.reply())
                .contains("Bến Tre", "Dz Hoàng", "#room-401", "Xem phòng này")
                .doesNotContain("Bạn muốn tìm");
        assertThat(followUp.reply())
                .contains("Bến Tre", "Dz Hoàng", "#room-401", "Xem phòng này")
                .doesNotContain("Bạn muốn đặt phòng? Hãy chọn điểm đến");
    }

    @Test
    void newlyApprovedCityFromDatabaseWorksWithoutStaticDestinationAlias() {
        Accommodation hotel = accommodation(41L, "Mekong Garden Hotel", "Trà Vinh", PropertyType.HOTEL, 350_000D);
        addRoomCapacity(hotel, 411L, 2);
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(hotel));
        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(PropertyType.HOTEL, ApprovalStatus.APPROVED))
                .thenReturn(List.of(hotel));
        when(availabilityService.checkAvailabilityForAccommodation(eq(hotel), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(availableRoom(411L, hotel, "Deluxe Mekong", 350_000, 4)));

        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Tôi muốn đặt khách sạn tại Trà Vinh", null);

        assertThat(response.intent()).isEqualTo("FIND_ACCOMMODATION");
        assertThat(response.reply())
                .contains("Trà Vinh", "Mekong Garden Hotel", "Deluxe Mekong", "350.000đ", "#room-411")
                .doesNotContain("Bạn muốn tìm");
        assertThat(response.quickReplies()).contains("Đặt phòng Trà Vinh");
    }

    @Test
    void cityRemovedFromApprovedDatabaseIsNotTreatedAsHardcodedDestination() {
        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Tôi muốn đặt khách sạn tại Trà Vinh", null);

        assertThat(response.intent()).isEqualTo("FIND_ACCOMMODATION");
        assertThat(response.reply()).contains("Bạn muốn tìm");
        assertThat(response.quickReplies()).doesNotContain("Trà Vinh", "Đặt phòng Trà Vinh");
    }

    @Test
    void groqContextUsesCurrentCancellationPolicyAndCannotReviveOldRefundWording() {
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of());
        when(voucherRepository.findByVoucherScopeOrderByCreatedAtDesc(VoucherScope.USER_GLOBAL))
                .thenReturn(List.of());

        String context = ReflectionTestUtils.invokeMethod(
                chatbotService, "buildGroqBusinessContext",
                "Tôi đang thất tình", "toi dang that tinh", null);

        assertThat(context)
                .contains("mất toàn bộ tiền cọc", "dự kiến hoàn 70%", "giữ 30% phí hủy")
                .doesNotContain("hoàn toàn bộ", "24 giờ", "từng cơ sở");
    }

    @Test
    void groqPromptForcesVietnameseWithDiacriticsAndTravelMateRedirect() {
        String prompt = ReflectionTestUtils.invokeMethod(chatbotService, "groqSystemPrompt");

        assertThat(prompt)
                .contains("tiếng Việt có dấu")
                .contains("Mình chưa có dữ liệu TravelMate để trả lời phần này.")
                .contains("bẻ lái sang gợi ý điểm đến");
        assertThat(prompt)
                .doesNotContain("Minh chua co du lieu")
                .doesNotContain("Chi tra loi bang tieng Viet");
    }

    @Test
    void knownUnaccentedAiFallbackIsNormalized() {
        String normalized = ReflectionTestUtils.invokeMethod(
                chatbotService,
                "normalizeKnownVietnameseFallbacks",
                "Minh chua co du lieu TravelMate de tra loi phan nay.");

        assertThat(normalized).isEqualTo("Mình chưa có dữ liệu TravelMate để trả lời phần này.");

        String destinationNames = ReflectionTestUtils.invokeMethod(
                chatbotService,
                "normalizeKnownVietnameseFallbacks",
                "Nen di Hoi An, Da Nang hoac Phu Quoc de nghi duong.");
        assertThat(destinationNames)
                .contains("Hội An", "Đà Nẵng", "Phú Quốc", "nghỉ dưỡng")
                .doesNotContain("Hoi An", "Da Nang", "Phu Quoc", "nghi duong");
    }

    @Test
    void bookingAndPaymentWordingMatchesActualFlow() {
        ChatbotService.ChatbotResponse policy =
                chatbotService.processMessage("Chính sách cọc 30%", null);
        ChatbotService.ChatbotResponse payment =
                chatbotService.processMessage("Thông tin thanh toán VNPAY", null);

        assertThat(policy.reply())
                .doesNotContain("phòng được giữ ngay lập tức")
                .contains("giữ phòng/căn trên hệ thống");
        assertThat(payment.reply())
                .doesNotContain("MoMo")
                .doesNotContain("ZaloPay")
                .contains("VNPAY Sandbox")
                .contains("thẻ test");
    }

    @Test
    void outOfScopeRedirectsBackToTravelMate() {
        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Viết code Java giúp tôi", null);

        assertThat(response.intent()).isEqualTo("OUT_OF_SCOPE");
        assertThat(response.reply())
                .contains("du lịch và đặt phòng")
                .contains("các nơi lưu trú đang mở bán")
                .doesNotContain("Đà Lạt", "Nha Trang");
    }

    @Test
    void currentNewsQuestionRedirectsToTravelMateWhenAiIsUnavailable() {
        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("thời sự hôm nay", null);

        assertThat(response.intent()).isEqualTo("AI_BUSINESS");
        assertThat(response.reply())
                .contains("TravelMate")
                .contains("Xem tất cả")
                .doesNotContain("Đà Lạt")
                .doesNotContain("Minh chua co du lieu")
                .doesNotContain("Mình chưa có dữ liệu TravelMate");
    }

    @Test
    void casualLifeContextCanFlowToTravelAiFallbackWhenConfigured() {
        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Hôm nay tôi đói", null);

        assertThat(response.intent()).isNotEqualTo("OUT_OF_SCOPE");
        assertThat(response.reply()).doesNotContain("chỉ hỗ trợ");
    }

    @Test
    void breakupMoodCanFlowToTravelAiFallbackWhenConfigured() {
        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Tôi đang thất tình, muốn đi đâu để gặp người mới?", null);

        assertThat(response.intent()).isNotEqualTo("OUT_OF_SCOPE");
        assertThat(response.reply()).doesNotContain("chỉ hỗ trợ");
    }

    @Test
    void outOfScopeWinsEvenWhenMessageMentionsBookingOrDestination() {
        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("Viết code đặt phòng Đà Lạt giúp tôi", null);

        assertThat(response.intent()).isEqualTo("OUT_OF_SCOPE");
        assertThat(response.reply()).contains("du lịch và đặt phòng");
    }

    private Accommodation accommodation(Long id, String name, String city,
                                        PropertyType type, Double minPrice) {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(id);
        accommodation.setName(name);
        accommodation.setCity(city);
        accommodation.setPropertyType(type);
        accommodation.setApprovalStatus(ApprovalStatus.APPROVED);
        accommodation.setThumbnailUrl("/assets/images/homestay-sapa.jpg");
        accommodation.setRating(8.8D);
        accommodation.setMinPrice(minPrice);
        return accommodation;
    }

    private RoomAvailabilityDto availableRoom(
            Long roomId, Accommodation accommodation, String roomName, long price, int available) {
        return new RoomAvailabilityDto(
                roomId, "R-" + roomId, roomName, "STANDARD",
                accommodation.getId(), accommodation.getName(), accommodation.getCity(),
                accommodation.getPropertyType().name(), "Đối tác",
                BigDecimal.valueOf(price), available + 1, 1, available);
    }

    private Room addRoomCapacity(Accommodation accommodation, Long roomId, int capacity) {
        Room room = new Room();
        room.setId(roomId);
        room.setCapacity(capacity);
        accommodation.setRooms(List.of(room));
        return room;
    }
}
