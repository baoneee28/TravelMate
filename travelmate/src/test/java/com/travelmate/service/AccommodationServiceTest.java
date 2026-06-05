package com.travelmate.service;

import com.travelmate.entity.Accommodation;
import com.travelmate.entity.Room;
import com.travelmate.entity.enums.ApprovalStatus;
import com.travelmate.entity.enums.PropertyType;
import com.travelmate.repository.AccommodationRepository;
import com.travelmate.repository.AmenityRepository;
import com.travelmate.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccommodationService - Admin duyệt listing/phòng")
class AccommodationServiceTest {

    @Mock private AccommodationRepository accommodationRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private AmenityRepository amenityRepository;

    private AccommodationService accommodationService;

    @BeforeEach
    void setUp() {
        accommodationService = new AccommodationService(accommodationRepository, roomRepository, amenityRepository);
    }

    @Test
    @DisplayName("Admin không duyệt phòng khi thiếu accommodation cha")
    void approveRoom_rejectsWhenParentMissing() {
        Room room = pendingRoom(10L, null);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> accommodationService.approveRoom(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("cơ sở lưu trú cha")
                .hasMessageContaining("không xác định");

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Admin không duyệt phòng khi accommodation cha chưa duyệt")
    void approveRoom_rejectsWhenParentPending() {
        Accommodation accommodation = accommodation("Da Lat Mountain Boutique Hotel", ApprovalStatus.PENDING);
        Room room = pendingRoom(11L, accommodation);
        when(roomRepository.findById(11L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> accommodationService.approveRoom(11L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chưa được duyệt")
                .hasMessageContaining("Da Lat Mountain Boutique Hotel");

        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Admin duyệt phòng hợp lệ thì mở bán online")
    void approveRoom_marksApprovedAndOpenForBooking() {
        Accommodation accommodation = accommodation("LATA Hotel & Apartments", ApprovalStatus.APPROVED);
        Room room = pendingRoom(12L, accommodation);
        room.setAvailableForBooking(false);
        when(roomRepository.findById(12L)).thenReturn(Optional.of(room));
        when(roomRepository.save(room)).thenReturn(room);

        Room result = accommodationService.approveRoom(12L);

        assertThat(result.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(result.getAvailableForBooking()).isTrue();
    }

    @Test
    @DisplayName("User chỉ thấy cơ sở có ít nhất một phòng đã duyệt và đang mở bán")
    void searchByType_hidesAccommodationWithoutApprovedOpenRoom() {
        Accommodation pendingOnly = accommodation("TravelMate Vippro", ApprovalStatus.APPROVED);
        Accommodation closedRoomOnly = accommodation("Da Lat Quiet Hotel", ApprovalStatus.APPROVED);
        Accommodation visible = accommodation("LATA Hotel & Apartments", ApprovalStatus.APPROVED);

        Room closedRoom = approvedRoom(21L, closedRoomOnly);
        closedRoom.setAvailableForBooking(false);
        Room openRoom = approvedRoom(22L, visible);

        when(accommodationRepository.findByPropertyTypeAndApprovalStatus(PropertyType.HOTEL, ApprovalStatus.APPROVED))
                .thenReturn(List.of(pendingOnly, closedRoomOnly, visible));
        when(roomRepository.findByAccommodationAndApprovalStatus(pendingOnly, ApprovalStatus.APPROVED))
                .thenReturn(List.of());
        when(roomRepository.findByAccommodationAndApprovalStatus(closedRoomOnly, ApprovalStatus.APPROVED))
                .thenReturn(List.of(closedRoom));
        when(roomRepository.findByAccommodationAndApprovalStatus(visible, ApprovalStatus.APPROVED))
                .thenReturn(List.of(openRoom));

        List<Accommodation> result = accommodationService.searchByType(PropertyType.HOTEL, "Đà Lạt");

        assertThat(result).containsExactly(visible);
    }

    private static Accommodation accommodation(String name, ApprovalStatus status) {
        Accommodation accommodation = new Accommodation();
        accommodation.setName(name);
        accommodation.setCity("Đà Lạt");
        accommodation.setPropertyType(PropertyType.HOTEL);
        accommodation.setApprovalStatus(status);
        return accommodation;
    }

    private static Room pendingRoom(Long id, Accommodation accommodation) {
        Room room = new Room();
        room.setId(id);
        room.setRoomCode("ROOM-" + id);
        room.setAccommodation(accommodation);
        room.setApprovalStatus(ApprovalStatus.PENDING);
        return room;
    }

    private static Room approvedRoom(Long id, Accommodation accommodation) {
        Room room = pendingRoom(id, accommodation);
        room.setApprovalStatus(ApprovalStatus.APPROVED);
        room.setAvailableForBooking(true);
        return room;
    }
}
