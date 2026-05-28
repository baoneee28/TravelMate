package com.travelmate.service;

import com.travelmate.entity.Room;
import com.travelmate.entity.RoomImage;
import com.travelmate.repository.RoomImageRepository;
import com.travelmate.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomImageService — Ảnh phòng/căn chính thức")
class RoomImageServiceTest {

    @Mock private RoomImageRepository roomImageRepository;
    @Mock private RoomRepository roomRepository;

    private RoomImageService roomImageService;

    @BeforeEach
    void setUp() {
        roomImageService = new RoomImageService(roomImageRepository, roomRepository);
    }

    @Test
    @DisplayName("Admin lưu ảnh mới → đồng bộ ảnh đại diện về rooms.imageUrl")
    void replaceImagesByAdmin_savesImagesAndSyncsPrimaryImage() {
        Room room = new Room();
        room.setId(7L);
        room.setRoomName("Deluxe View Hồ");
        when(roomRepository.findById(7L)).thenReturn(Optional.of(room));
        when(roomImageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RoomImage> images = roomImageService.replaceImagesByAdmin(
                7L,
                List.of("https://cdn.travelmate.vn/room-1.jpg", "https://cdn.travelmate.vn/room-2.webp"),
                List.of("Góc ngủ", "Ban công"),
                1);

        assertThat(images).hasSize(2);
        assertThat(images.get(1).getPrimaryImage()).isTrue();
        assertThat(room.getImageUrl()).isEqualTo("https://cdn.travelmate.vn/room-2.webp");
        verify(roomImageRepository).deleteByRoom(room);
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("URL .pdf/.exe → bị chặn, không lưu vào DB")
    void replaceImagesByAdmin_rejectsUnsupportedImageExtension() {
        Room room = new Room();
        room.setId(8L);
        when(roomRepository.findById(8L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomImageService.replaceImagesByAdmin(
                8L,
                List.of("https://cdn.travelmate.vn/room-brochure.pdf"),
                List.of("Sai định dạng"),
                0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chỉ hỗ trợ JPG, PNG, WEBP");

        verify(roomImageRepository, never()).deleteByRoom(any(Room.class));
        verify(roomImageRepository, never()).saveAll(anyList());
        verify(roomRepository, never()).save(room);
    }

    @Test
    @DisplayName("URL CDN không có đuôi rõ ràng vẫn được phép để dùng link ảnh thực tế")
    void replaceImagesByAdmin_allowsCdnUrlWithoutExtension() {
        Room room = new Room();
        room.setId(9L);
        room.setRoomName("Premier Suite");
        when(roomRepository.findById(9L)).thenReturn(Optional.of(room));
        when(roomImageRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RoomImage> images = roomImageService.replaceImagesByAdmin(
                9L,
                List.of("https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop"),
                List.of("Ảnh CDN"),
                0);

        assertThat(images).hasSize(1);
        assertThat(room.getImageUrl()).contains("images.unsplash.com");
    }
}
