package com.travelmate.controller.page;

import com.travelmate.entity.TravelPost;
import com.travelmate.repository.UserRepository;
import com.travelmate.service.SupportTicketService;
import com.travelmate.service.TravelDestinationService;
import com.travelmate.service.TravelPostService;
import com.travelmate.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomePageControllerNewsTest {

    @Mock private VoucherService voucherService;
    @Mock private SupportTicketService supportTicketService;
    @Mock private UserRepository userRepository;
    @Mock private TravelPostService travelPostService;
    @Mock private TravelDestinationService travelDestinationService;

    private HomePageController controller;

    @BeforeEach
    void setUp() {
        controller = new HomePageController(
                voucherService, supportTicketService, userRepository,
                travelPostService, travelDestinationService);
    }

    @Test
    void newsPageFiltersByDestinationOnBackendAndHidesFeaturedLayout() {
        TravelPost daLat = post("Đà Lạt cuối tuần", "Đà Lạt");
        TravelPost daNang = post("Đà Nẵng mùa hè", "Đà Nẵng");
        when(travelPostService.getVisiblePosts()).thenReturn(List.of(daLat, daNang));
        when(travelPostService.getVisibleByDestination("dalat")).thenReturn(List.of(daLat));
        when(travelPostService.getDestinationDisplayName("dalat")).thenReturn("Đà Lạt");

        Model model = new ExtendedModelMap();
        String view = controller.news(null, "dalat", "", model);

        assertThat(view).isEqualTo("user/news");
        assertThat(model.getAttribute("newsFiltered")).isEqualTo(true);
        assertThat(model.getAttribute("newsFilterLabel")).isEqualTo("Đà Lạt");
        assertThat(model.getAttribute("newsPosts")).isEqualTo(List.of(daLat));
        assertThat(model.getAttribute("featuredPost")).isNull();
        assertThat(model.getAttribute("sidebarPosts")).isEqualTo(List.of());
        assertThat(model.getAttribute("gridPosts")).isEqualTo(List.of());
        assertThat(model.getAttribute("newsLocations")).isEqualTo(List.of("Đà Lạt", "Đà Nẵng"));
        verify(travelPostService).getVisibleByDestination("dalat");
    }

    private static TravelPost post(String title, String destination) {
        TravelPost post = new TravelPost();
        post.setTitle(title);
        post.setDestination(destination);
        post.setStatus(TravelPost.Status.VISIBLE);
        return post;
    }
}
