package com.travelmate.service;

import com.travelmate.entity.Room;
import com.travelmate.entity.RoomImage;
import com.travelmate.repository.RoomImageRepository;
import com.travelmate.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoomImageService {

    private static final int MAX_IMAGES_PER_ROOM = 3;

    private final RoomImageRepository roomImageRepository;
    private final RoomRepository roomRepository;

    public RoomImageService(RoomImageRepository roomImageRepository, RoomRepository roomRepository) {
        this.roomImageRepository = roomImageRepository;
        this.roomRepository = roomRepository;
    }

    public List<RoomImage> getImages(Room room) {
        return roomImageRepository.findByRoomOrderByPrimaryImageDescSortOrderAscIdAsc(room);
    }

    public Map<Long, List<RoomImage>> getImagesForRooms(List<Room> rooms) {
        Map<Long, List<RoomImage>> result = new LinkedHashMap<>();
        if (rooms == null) {
            return result;
        }
        for (Room room : rooms) {
            result.put(room.getId(), getImages(room));
        }
        return result;
    }

    /**
     * Admin thay toàn bộ ảnh hiển thị. Ảnh đại diện được đồng bộ về
     * rooms.image_url để các màn hình cũ vẫn hiển thị nhất quán.
     */
    @Transactional
    public List<RoomImage> replaceImagesByAdmin(Long roomId, List<String> imageUrls,
                                                 List<String> captions, int primaryIndex) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Phòng/căn không tồn tại."));

        List<String> cleanedUrls = new ArrayList<>();
        Set<String> usedUrls = new LinkedHashSet<>();
        if (imageUrls != null) {
            for (String raw : imageUrls) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String url = raw.trim();
                validateImageUrl(url);
                if (usedUrls.add(url)) {
                    cleanedUrls.add(url);
                }
            }
        }
        if (cleanedUrls.isEmpty()) {
            throw new IllegalArgumentException("Cần nhập ít nhất một URL ảnh để hiển thị.");
        }
        if (cleanedUrls.size() > MAX_IMAGES_PER_ROOM) {
            throw new IllegalArgumentException("Mỗi phòng/căn được phép tối đa 3 ảnh hiển thị.");
        }

        int chosenIndex = primaryIndex >= 0 && primaryIndex < cleanedUrls.size() ? primaryIndex : 0;
        roomImageRepository.deleteByRoom(room);

        List<RoomImage> entities = new ArrayList<>();
        for (int i = 0; i < cleanedUrls.size(); i++) {
            RoomImage image = new RoomImage();
            image.setRoom(room);
            image.setImageUrl(cleanedUrls.get(i));
            String caption = captions != null && i < captions.size() ? captions.get(i) : null;
            image.setCaption(caption != null && !caption.isBlank()
                    ? caption.trim()
                    : room.getRoomName() + " - ảnh " + (i + 1));
            image.setSortOrder(i);
            image.setPrimaryImage(i == chosenIndex);
            entities.add(image);
        }
        room.setImageUrl(cleanedUrls.get(chosenIndex));
        roomRepository.save(room);
        return roomImageRepository.saveAll(entities);
    }

    private void validateImageUrl(String url) {
        if (!(url.startsWith("https://") || url.startsWith("http://")
                || url.startsWith("/assets/") || url.startsWith("/uploads/"))) {
            throw new IllegalArgumentException("Ảnh phải là URL http(s), /assets/ hoặc /uploads/ hợp lệ.");
        }
        String cleanPath = url.split("[?#]", 2)[0].toLowerCase();
        int slashIndex = cleanPath.lastIndexOf('/');
        int dotIndex = cleanPath.lastIndexOf('.');
        if (dotIndex > slashIndex) {
            String extension = cleanPath.substring(dotIndex);
            if (!extension.matches("\\.(jpg|jpeg|png|webp)")) {
                throw new IllegalArgumentException("Chỉ hỗ trợ JPG, PNG, WEBP.");
            }
        }
    }
}
