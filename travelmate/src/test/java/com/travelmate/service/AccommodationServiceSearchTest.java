package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.AmenityRepository;
import com.travelmate.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccommodationServiceSearchTest {

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private AmenityRepository amenityRepository;

    private AccommodationService accommodationService;

    @BeforeEach
    void setUp() {
        accommodationService = new AccommodationService(
                accommodationRepository,
                roomRepository,
                amenityRepository
        );
    }

    @Test
    void searchByTypeMatchesVietnameseDestinationsWithoutAccents() {
        Accommodation daLatHotel = accommodation("LATA Hotel & Apartments", "Đà Lạt");
        Accommodation daNangHotel = accommodation("Han River Hotel", "Đà Nẵng");
        Accommodation nhaTrangHotel = accommodation("Nha Trang City Hotel", "Nha Trang");
        Accommodation hoiAnHotel = accommodation("Hoi An Lantern Homestay", "Hội An");
        Accommodation saPaHotel = accommodation("Sapa Cloud Valley Hotel", "Sa Pa");
        Accommodation haGiangHotel = accommodation("Ha Giang Loop Lodge", "Hà Giang");
        Accommodation ninhBinhHotel = accommodation("Trang An Garden Hotel", "Ninh Bình");
        Accommodation haNoiHotel = accommodation("Old Quarter Stay", "Hà Nội");
        Accommodation phuQuocHotel = accommodation("Sunset Pearl Resort", "Phú Quốc");
        Accommodation hcmHotel = accommodation("Ben Thanh Urban Hotel", "TP. Hồ Chí Minh");
        Accommodation quangNinhHotel = accommodation("Ha Long Bay View Hotel", "Quảng Ninh");
        Accommodation yenBaiHotel = accommodation("Thac Ba Lake Retreat", "Yên Bái");
        Accommodation sonLaHotel = accommodation("Moc Chau Plateau Lodge", "Sơn La");

        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(
                PropertyType.HOTEL, ApprovalStatus.APPROVED))
                .thenReturn(List.of(
                        daLatHotel,
                        daNangHotel,
                        nhaTrangHotel,
                        hoiAnHotel,
                        saPaHotel,
                        haGiangHotel,
                        ninhBinhHotel,
                        haNoiHotel,
                        phuQuocHotel,
                        hcmHotel,
                        quangNinhHotel,
                        yenBaiHotel,
                        sonLaHotel
                ));

        assertSearch("Đà Lạt", "LATA Hotel & Apartments");
        assertSearch("da lat", "LATA Hotel & Apartments");
        assertSearch("dalat", "LATA Hotel & Apartments");
        assertSearch("   dA    LaT   ", "LATA Hotel & Apartments");
        assertSearch("Đà Nẵng", "Han River Hotel");
        assertSearch("da nang", "Han River Hotel");
        assertSearch("danang", "Han River Hotel");
        assertSearch("NHA TRANG", "Nha Trang City Hotel");
        assertSearch("nhatrang", "Nha Trang City Hotel");
        assertSearch("Hội An", "Hoi An Lantern Homestay");
        assertSearch("hoian", "Hoi An Lantern Homestay");
        assertSearch("Sa Pa", "Sapa Cloud Valley Hotel");
        assertSearch("sapa", "Sapa Cloud Valley Hotel");
        assertSearch("Lào Cai", "Sapa Cloud Valley Hotel");
        assertSearch("lao cai", "Sapa Cloud Valley Hotel");
        assertSearch("hagiang", "Ha Giang Loop Lodge");
        assertSearch("ninhbinh", "Trang An Garden Hotel");
        assertSearch("hanoi", "Old Quarter Stay");
        assertSearch("phuquoc", "Sunset Pearl Resort");
        assertSearch("TP.HCM", "Ben Thanh Urban Hotel");
        assertSearch("tphcm", "Ben Thanh Urban Hotel");
        assertSearch("hcm", "Ben Thanh Urban Hotel");
        assertSearch("Sài Gòn", "Ben Thanh Urban Hotel");
        assertSearch("ho chi minh", "Ben Thanh Urban Hotel");
        assertSearch("quangninh", "Ha Long Bay View Hotel");
        assertSearch("yenbai", "Thac Ba Lake Retreat");
        assertSearch("sonla", "Moc Chau Plateau Lodge");

        assertThat(accommodationService.searchByType(PropertyType.HOTEL, "abcxyz")).isEmpty();
    }

    @Test
    void searchByTypeMatchesCanThoAliasForSeededResortDemo() {
        Accommodation canThoResort = accommodation("Azerai Cần Thơ Resort", "Cần Thơ", PropertyType.RESORT);

        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(
                PropertyType.RESORT, ApprovalStatus.APPROVED))
                .thenReturn(List.of(canThoResort));

        assertThat(accommodationService.searchByType(PropertyType.RESORT, "cantho"))
                .extracting(Accommodation::getName)
                .containsExactly("Azerai Cần Thơ Resort");
        assertThat(accommodationService.searchByType(PropertyType.RESORT, "can tho"))
                .extracting(Accommodation::getName)
                .containsExactly("Azerai Cần Thơ Resort");
        assertThat(accommodationService.searchByType(PropertyType.RESORT, "Cần Thơ"))
                .extracting(Accommodation::getName)
                .containsExactly("Azerai Cần Thơ Resort");
    }

    private void assertSearch(String keyword, String expectedAccommodationName) {
        assertThat(accommodationService.searchByType(PropertyType.HOTEL, keyword))
                .extracting(Accommodation::getName)
                .containsExactly(expectedAccommodationName);
    }

    private static Accommodation accommodation(String name, String city) {
        return accommodation(name, city, PropertyType.HOTEL);
    }

    private static Accommodation accommodation(String name, String city, PropertyType propertyType) {
        Accommodation accommodation = new Accommodation();
        accommodation.setName(name);
        accommodation.setCity(city);
        accommodation.setPropertyType(propertyType);
        accommodation.setApprovalStatus(ApprovalStatus.APPROVED);
        return accommodation;
    }
}
