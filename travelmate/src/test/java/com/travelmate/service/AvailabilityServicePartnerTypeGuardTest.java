package com.travelmate.service;

import com.travelmate.dto.RoomAvailabilityDto;
import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.User;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.BookingRepository;
import com.travelmate.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AvailabilityService — Partner chỉ thấy đúng loại lưu trú")
class AvailabilityServicePartnerTypeGuardTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private AccommodationRepository accommodationRepository;
    @Mock private RoomRepository roomRepository;

    private AvailabilityService availabilityService;

    @BeforeEach
    void setUp() {
        availabilityService = new AvailabilityService(
                bookingRepository,
                accommodationRepository,
                roomRepository
        );
    }

    @Test
    void checkAvailabilityForPartnerFiltersWrongPropertyType() {
        User resortPartner = partner(4L, PropertyType.RESORT);
        Accommodation wrongHotel = accommodation(20L, "InterContinental Nha Trang", PropertyType.HOTEL, resortPartner);
        wrongHotel.setRooms(List.of(room(30L, "ICN-STD", wrongHotel)));
        Accommodation resort = accommodation(21L, "Vinpearl Resort & Spa Nha Trang", PropertyType.RESORT, resortPartner);
        resort.setRooms(List.of(room(31L, "VPR-DLX", resort)));

        when(accommodationRepository.findByOwnerAndApprovalStatus(resortPartner, ApprovalStatus.APPROVED))
                .thenReturn(List.of(wrongHotel, resort));
        when(bookingRepository.sumActiveBookingsByRoom(anyList())).thenReturn(List.of());
        when(bookingRepository.sumOverlappingBookingsByRoom(
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                anyList())).thenReturn(List.of());

        List<RoomAvailabilityDto> result = availabilityService.checkAvailabilityForPartner(
                resortPartner, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        assertThat(result)
                .extracting(RoomAvailabilityDto::getAccommodationName)
                .containsExactly("Vinpearl Resort & Spa Nha Trang");
    }

    @Test
    @DisplayName("Partner HOMESTAY chỉ thấy phòng Homestay của mình")
    void checkAvailabilityForHomestayFiltersWrongPropertyType() {
        User homestayPartner = partner(8L, PropertyType.HOMESTAY);
        Accommodation wrongVilla = accommodation(4L, "The Anam Villa Nha Trang", PropertyType.VILLA, homestayPartner);
        wrongVilla.setRooms(List.of(room(18L, "ANM-GDN", wrongVilla)));
        Accommodation homestay = accommodation(7L, "Mộc Nhiên Garden Homestay Đà Lạt", PropertyType.HOMESTAY, homestayPartner);
        homestay.setRooms(List.of(room(23L, "MND-ATT", homestay)));

        when(accommodationRepository.findByOwnerAndApprovalStatus(homestayPartner, ApprovalStatus.APPROVED))
                .thenReturn(List.of(wrongVilla, homestay));
        when(bookingRepository.sumActiveBookingsByRoom(anyList())).thenReturn(List.of());
        when(bookingRepository.sumOverlappingBookingsByRoom(
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                anyList())).thenReturn(List.of());

        List<RoomAvailabilityDto> result = availabilityService.checkAvailabilityForPartner(
                homestayPartner, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        assertThat(result)
                .extracting(RoomAvailabilityDto::getAccommodationName)
                .containsExactly("Mộc Nhiên Garden Homestay Đà Lạt");
    }

    @Test
    void checkAvailabilityForAccommodationSkipsRoomsStoppedForOnlineBooking() {
        Accommodation hotel = accommodation(30L, "LATA Hotel & Apartments", PropertyType.HOTEL, partner(3L, PropertyType.HOTEL));
        Room stopped = room(41L, "LATA-STOP", hotel);
        stopped.setAvailableForBooking(false);
        Room open = room(42L, "LATA-OPEN", hotel);
        when(roomRepository.findByAccommodationAndApprovalStatus(hotel, ApprovalStatus.APPROVED))
                .thenReturn(List.of(stopped, open));
        when(bookingRepository.sumActiveBookingsByRoom(anyList())).thenReturn(List.of());
        when(bookingRepository.sumOverlappingBookingsByRoom(
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                org.mockito.ArgumentMatchers.any(LocalDate.class),
                anyList())).thenReturn(List.of());

        List<RoomAvailabilityDto> result = availabilityService.checkAvailabilityForAccommodation(
                hotel, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        assertThat(result)
                .extracting(RoomAvailabilityDto::getRoomCode)
                .containsExactly("LATA-OPEN");
    }

    private static User partner(Long id, PropertyType propertyType) {
        User partner = new User();
        partner.setId(id);
        partner.setRole(User.Role.PARTNER);
        partner.setPartnerPropertyType(propertyType);
        return partner;
    }

    private static Accommodation accommodation(Long id, String name, PropertyType propertyType, User owner) {
        Accommodation accommodation = new Accommodation();
        accommodation.setId(id);
        accommodation.setName(name);
        accommodation.setCity("Nha Trang");
        accommodation.setPropertyType(propertyType);
        accommodation.setApprovalStatus(ApprovalStatus.APPROVED);
        accommodation.setOwner(owner);
        return accommodation;
    }

    private static Room room(Long id, String roomCode, Accommodation accommodation) {
        Room room = new Room();
        room.setId(id);
        room.setRoomCode(roomCode);
        room.setRoomName(roomCode);
        room.setAccommodation(accommodation);
        room.setApprovalStatus(ApprovalStatus.APPROVED);
        room.setPricePerNight(new BigDecimal("1000000"));
        room.setAvailableQuantity(5);
        room.setAvailableForBooking(true);
        return room;
    }
}
