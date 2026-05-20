package com.travelmate.service;

import com.travelmate.entity.TravelPost;
import com.travelmate.repository.TravelPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelPostServiceTest {

    @Mock
    private TravelPostRepository travelPostRepository;

    @Test
    void normalizeDestinationSupportsVietnameseAccentsAndAliases() {
        TravelPostService service = new TravelPostService(travelPostRepository);

        assertThat(service.normalizeDestination("Đà Lạt")).isEqualTo("da-lat");
        assertThat(service.normalizeDestination("da lat")).isEqualTo("da-lat");
        assertThat(service.normalizeDestination("dalat")).isEqualTo("da-lat");
        assertThat(service.normalizeDestination("Da Lat")).isEqualTo("da-lat");
        assertThat(service.normalizeDestination("Nha Trang")).isEqualTo("nha-trang");
        assertThat(service.normalizeDestination("nha-trang")).isEqualTo("nha-trang");
        assertThat(service.normalizeDestination("Hội An")).isEqualTo("hoi-an");
        assertThat(service.normalizeDestination("hoi an")).isEqualTo("hoi-an");
        assertThat(service.normalizeDestination("Đà Nẵng")).isEqualTo("da-nang");
        assertThat(service.normalizeDestination("da nang")).isEqualTo("da-nang");
        assertThat(service.normalizeDestination("Sa Pa")).isEqualTo("sa-pa");
        assertThat(service.normalizeDestination("sapa")).isEqualTo("sa-pa");
        assertThat(service.normalizeDestination("Lào Cai")).isEqualTo("lao-cai");
        assertThat(service.normalizeDestination("lao cai")).isEqualTo("lao-cai");
        assertThat(service.normalizeDestination("Phú Quốc")).isEqualTo("phu-quoc");
        assertThat(service.normalizeDestination("phu quoc")).isEqualTo("phu-quoc");
        assertThat(service.normalizeDestination("Hà Giang")).isEqualTo("ha-giang");
        assertThat(service.normalizeDestination("hagiang")).isEqualTo("ha-giang");
        assertThat(service.normalizeDestination("Ninh Bình")).isEqualTo("ninh-binh");
        assertThat(service.normalizeDestination("ninhbinh")).isEqualTo("ninh-binh");
        assertThat(service.normalizeDestination("Hà Nội")).isEqualTo("ha-noi");
        assertThat(service.normalizeDestination("hanoi")).isEqualTo("ha-noi");
        assertThat(service.normalizeDestination("TP.HCM")).isEqualTo("ho-chi-minh");
        assertThat(service.normalizeDestination("tphcm")).isEqualTo("ho-chi-minh");
        assertThat(service.normalizeDestination("hcm")).isEqualTo("ho-chi-minh");
        assertThat(service.normalizeDestination("Sài Gòn")).isEqualTo("ho-chi-minh");
        assertThat(service.normalizeDestination("ho chi minh")).isEqualTo("ho-chi-minh");
        assertThat(service.normalizeDestination("Quảng Ninh")).isEqualTo("quang-ninh");
        assertThat(service.normalizeDestination("quangninh")).isEqualTo("quang-ninh");
        assertThat(service.normalizeDestination("Yên Bái")).isEqualTo("yen-bai");
        assertThat(service.normalizeDestination("yenbai")).isEqualTo("yen-bai");
        assertThat(service.normalizeDestination("Sơn La")).isEqualTo("son-la");
        assertThat(service.normalizeDestination("sonla")).isEqualTo("son-la");
        assertThat(service.normalizeDestination("Cần Thơ")).isEqualTo("can-tho");
        assertThat(service.normalizeDestination("cantho")).isEqualTo("can-tho");
    }

    @Test
    void getVisibleByDestinationQueriesRepositoryByNormalizedSlug() {
        TravelPostService service = new TravelPostService(travelPostRepository);
        List<TravelPost> posts = List.of(new TravelPost());
        when(travelPostRepository.findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of("da-lat"), TravelPost.Status.VISIBLE)).thenReturn(posts);

        assertThat(service.getVisibleByDestination("da lat")).isSameAs(posts);

        verify(travelPostRepository).findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of("da-lat"), TravelPost.Status.VISIBLE);
    }

    @Test
    void getVisibleByDestinationUsesRelatedSlugsForProvinceAliases() {
        TravelPostService service = new TravelPostService(travelPostRepository);
        List<TravelPost> posts = List.of(new TravelPost());
        when(travelPostRepository.findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of("lao-cai", "sa-pa"), TravelPost.Status.VISIBLE)).thenReturn(posts);

        assertThat(service.getVisibleByDestination("lao cai")).isSameAs(posts);

        verify(travelPostRepository).findByDestinationSlugInAndStatusOrderByCreatedAtDesc(
                Set.of("lao-cai", "sa-pa"), TravelPost.Status.VISIBLE);
    }

    @Test
    void displayNameUsesCanonicalVietnameseDestinationName() {
        TravelPostService service = new TravelPostService(travelPostRepository);

        assertThat(service.getDestinationDisplayName("dalat")).isEqualTo("Đà Lạt");
        assertThat(service.getDestinationDisplayName("sapa")).isEqualTo("Sa Pa");
        assertThat(service.getDestinationDisplayName("phu quoc")).isEqualTo("Phú Quốc");
        assertThat(service.getDestinationDisplayName("hcm")).isEqualTo("TP. Hồ Chí Minh");
        assertThat(service.getDestinationDisplayName("sai gon")).isEqualTo("TP. Hồ Chí Minh");
        assertThat(service.getDestinationDisplayName("lao cai")).isEqualTo("Lào Cai");
        assertThat(service.getDestinationDisplayName("quang ninh")).isEqualTo("Quảng Ninh");
        assertThat(service.getDestinationDisplayName("yenbai")).isEqualTo("Yên Bái");
        assertThat(service.getDestinationDisplayName("son la")).isEqualTo("Sơn La");
        assertThat(service.getDestinationDisplayName("can tho")).isEqualTo("Cần Thơ");
        assertThat(service.getDestinationDisplayName("binh duong")).isEqualTo("binh duong");
    }
}
