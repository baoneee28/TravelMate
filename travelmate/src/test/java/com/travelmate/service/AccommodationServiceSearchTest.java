package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Amenity;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void searchByTypeMatchesCanThoAliasForResortDemo() {
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

    @Test
    void searchByTypeReturnsEmptyStateForDeepTyposInsteadOfWrongResults() {
        Accommodation daLatHotel = accommodation("LATA Hotel & Apartments", "Đà Lạt");
        Accommodation daNangHotel = accommodation("Han River Hotel", "Đà Nẵng");
        Accommodation hcmHotel = accommodation("Ben Thanh Urban Hotel", "TP. Hồ Chí Minh");

        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(
                PropertyType.HOTEL, ApprovalStatus.APPROVED))
                .thenReturn(List.of(daLatHotel, daNangHotel, hcmHotel));

        assertThat(accommodationService.searchByType(PropertyType.HOTEL, "đlat"))
                .extracting(Accommodation::getName)
                .containsExactly("LATA Hotel & Apartments");
        assertThat(accommodationService.searchByType(PropertyType.HOTEL, "dalatt")).isEmpty();
        assertThat(accommodationService.searchByType(PropertyType.HOTEL, "sgon")).isEmpty();
        assertThat(accommodationService.searchByType(PropertyType.HOTEL, "da nangg")).isEmpty();
    }

    @Test
    void partnerAccommodationListOnlyIncludesRegisteredPropertyType() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Accommodation wrongHotel = accommodation("InterContinental Nha Trang", "Nha Trang", PropertyType.HOTEL);
        wrongHotel.setOwner(resortPartner);
        Accommodation resort = accommodation("Vinpearl Resort & Spa Nha Trang", "Nha Trang", PropertyType.RESORT);
        resort.setOwner(resortPartner);

        when(accommodationRepository.findByOwner(resortPartner))
                .thenReturn(List.of(wrongHotel, resort));

        assertThat(accommodationService.getAccommodationsByOwner(resortPartner))
                .extracting(Accommodation::getName)
                .containsExactly("Vinpearl Resort & Spa Nha Trang");
    }

    @Test
    void partnerApprovedRoomsOnlyIncludesRoomsFromRegisteredPropertyType() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Room hotelRoom = room("ICN-STD", accommodation("InterContinental Nha Trang", "Nha Trang", PropertyType.HOTEL));
        hotelRoom.getAccommodation().setOwner(resortPartner);
        Room resortRoom = room("VPR-DLX", accommodation("Vinpearl Resort & Spa Nha Trang", "Nha Trang", PropertyType.RESORT));
        resortRoom.getAccommodation().setOwner(resortPartner);

        when(roomRepository.findByAccommodation_OwnerAndApprovalStatus(resortPartner, ApprovalStatus.APPROVED))
                .thenReturn(List.of(hotelRoom, resortRoom));

        assertThat(accommodationService.getApprovedRoomsForPartner(resortPartner))
                .extracting(Room::getRoomCode)
                .containsExactly("VPR-DLX");
    }

    @Test
    void updateRoomAmenitiesRejectsRoomFromWrongPropertyTypeEvenWhenOwnerMatches() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Room hotelRoom = room("ICN-STD", accommodation("InterContinental Nha Trang", "Nha Trang", PropertyType.HOTEL));
        hotelRoom.setId(10L);
        hotelRoom.getAccommodation().setOwner(resortPartner);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(hotelRoom));

        assertThatThrownBy(() -> accommodationService.updateRoomAmenities(resortPartner, 10L, List.of()))
                .hasMessageContaining("Tài khoản RESORT");
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    void updateRoomAmenitiesReturnsApprovedRoomToPendingWhenImportantAmenityIsRemoved() {
        User hotelPartner = partner(3L, PropertyType.HOTEL);
        Accommodation hotel = accommodation("LATA Hotel & Apartments", "Đà Lạt", PropertyType.HOTEL);
        hotel.setOwner(hotelPartner);
        Room room = room("LATA-DLX", hotel);
        room.setId(11L);
        Amenity wifi = amenity(1L, "WiFi miễn phí");
        Amenity television = amenity(2L, "TV màn hình phẳng");
        room.setAmenities(List.of(wifi, television));

        when(roomRepository.findById(11L)).thenReturn(Optional.of(room));
        when(amenityRepository.findAllById(List.of(2L))).thenReturn(List.of(television));
        when(roomRepository.save(room)).thenReturn(room);

        Room updated = accommodationService.updateRoomAmenities(hotelPartner, 11L, List.of(2L));

        assertThat(updated.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        assertThat(updated.getAvailableForBooking()).isFalse();
        verify(roomRepository).save(room);
    }

    @Test
    void userRoomListsHideRoomsStoppedForOnlineBooking() {
        Accommodation hotel = accommodation("LATA Hotel & Apartments", "Đà Lạt", PropertyType.HOTEL);
        Room open = room("LATA-OPEN", hotel);
        open.setAvailableQuantity(2);
        Room stopped = room("LATA-STOP", hotel);
        stopped.setAvailableQuantity(2);
        stopped.setAvailableForBooking(false);

        when(roomRepository.findByAccommodationAndAvailableQuantityGreaterThanAndApprovalStatus(
                hotel, 0, ApprovalStatus.APPROVED)).thenReturn(List.of(stopped, open));
        when(roomRepository.findByAccommodationAndApprovalStatus(hotel, ApprovalStatus.APPROVED))
                .thenReturn(List.of(stopped, open));

        assertThat(accommodationService.getAvailableRooms(hotel))
                .extracting(Room::getRoomCode)
                .containsExactly("LATA-OPEN");
        assertThat(accommodationService.getAllApprovedRooms(hotel))
                .extracting(Room::getRoomCode)
                .containsExactly("LATA-OPEN");
    }

    @Test
    void partnerCanToggleRoomSellingWithoutChangingQuota() {
        User hotelPartner = partner(3L, PropertyType.HOTEL);
        Accommodation hotel = accommodation("LATA Hotel & Apartments", "Đà Lạt", PropertyType.HOTEL);
        hotel.setOwner(hotelPartner);
        Room room = room("LATA-STD", hotel);
        room.setId(77L);
        room.setAvailableQuantity(5);
        room.setAvailableForBooking(true);
        when(roomRepository.findById(77L)).thenReturn(Optional.of(room));
        when(roomRepository.save(room)).thenReturn(room);

        Room result = accommodationService.toggleRoomBookingAvailability(hotelPartner, 77L);

        assertThat(result.getAvailableForBooking()).isFalse();
        assertThat(result.getAvailableQuantity()).isEqualTo(5);
        verify(roomRepository).save(room);
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

    private static User partner(Long id, PropertyType propertyType) {
        User partner = new User();
        partner.setId(id);
        partner.setRole(User.Role.PARTNER);
        partner.setPartnerPropertyType(propertyType);
        return partner;
    }

    private static Room room(String roomCode, Accommodation accommodation) {
        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setRoomName(roomCode);
        room.setAccommodation(accommodation);
        room.setApprovalStatus(ApprovalStatus.APPROVED);
        room.setAvailableForBooking(true);
        return room;
    }

    private static Amenity amenity(Long id, String name) {
        Amenity amenity = new Amenity();
        amenity.setId(id);
        amenity.setName(name);
        return amenity;
    }
}
