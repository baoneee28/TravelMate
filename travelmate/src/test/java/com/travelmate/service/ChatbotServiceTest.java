package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                accommodationRepository,
                travelPostRepository,
                userRepository,
                bookingRepository,
                voucherRepository
        );
        lenient().when(travelPostRepository.findTop3ByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                anyCollection(), eq(com.travelmate.entity.TravelPost.Status.VISIBLE)))
                .thenReturn(List.of());
    }

    @Test
    void greetingUnderstandsShortGreetingAndCommonTypo() {
        assertThat(chatbotService.processMessage("helo", null).intent()).isEqualTo("GREETING");
        assertThat(chatbotService.processMessage("hi", null).intent()).isEqualTo("GREETING");
        assertThat(chatbotService.processMessage("halo", null).intent()).isEqualTo("GREETING");
    }

    @Test
    void budgetPlanUnderstandsMoneyAndDestinationBeforeFallback() {
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(
                        accommodation(1L, "Sapa Valley Homestay", "Sa Pa", PropertyType.HOMESTAY, 380_000D),
                        accommodation(2L, "Sapa Cloud Valley Hotel", "Sa Pa", PropertyType.HOTEL, 720_000D)
                ));

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
        when(accommodationRepository.findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus.APPROVED))
                .thenReturn(List.of(
                        accommodation(3L, "InterContinental Nha Trang", "Nha Trang", PropertyType.HOTEL, 1_850_000D),
                        accommodation(4L, "Novotel Đà Nẵng Premier", "Đà Nẵng", PropertyType.HOTEL, 1_100_000D),
                        accommodation(5L, "Sunset Pearl Resort Phú Quốc", "Phú Quốc", PropertyType.RESORT, 2_100_000D),
                        accommodation(6L, "Pullman Vũng Tàu", "Vũng Tàu", PropertyType.HOTEL, 1_650_000D)
                ));

        ChatbotService.ChatbotResponse response =
                chatbotService.processMessage("4 triệu muốn đi biển", null);

        assertThat(response.intent()).isEqualTo("BUDGET_TRAVEL_PLAN");
        assertThat(response.reply())
                .contains("du lịch biển")
                .contains("Nha Trang")
                .contains("Đà Nẵng")
                .contains("Phú Quốc")
                .contains("Xem nơi lưu trú")
                .contains("Xem gợi ý du lịch")
                .contains("Xem voucher");
    }

    @Test
    void bookingAndPaymentWordingMatchesActualFlow() {
        ChatbotService.ChatbotResponse policy =
                chatbotService.processMessage("Chính sách cọc 30%", null);
        ChatbotService.ChatbotResponse payment =
                chatbotService.processMessage("Thông tin thanh toán VNPAY", null);

        assertThat(policy.reply())
                .doesNotContain("phòng được giữ ngay lập tức")
                .contains("chuyển TravelMate/Partner xác nhận giữ phòng");
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
                .contains("Đà Lạt")
                .contains("Nha Trang");
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
}
