package com.travelmate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileStorageService — Validate ảnh upload")
class FileStorageServiceTest {

    private final FileStorageService fileStorageService = new FileStorageService();

    @Test
    @DisplayName("Upload .pdf/.exe → bị chặn trước khi lưu")
    void storeRoomImageRejectsUnsupportedFileExtension() {
        MockMultipartFile pdf = new MockMultipartFile(
                "imageFiles", "room.pdf", "application/pdf", "fake".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeRoomImage(pdf))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chỉ hỗ trợ JPG, PNG, WEBP");
    }

    @Test
    @DisplayName("Upload đuôi .jpg nhưng nội dung không phải ảnh → bị chặn")
    void storeRoomImageRejectsMismatchedContentType() {
        MockMultipartFile fakeJpg = new MockMultipartFile(
                "imageFiles", "room.jpg", "application/pdf", "fake".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeRoomImage(fakeJpg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không khớp nội dung file");
    }

    @Test
    @DisplayName("Upload ảnh quá 10MB → bị chặn")
    void storeRoomImageRejectsOversizedFile() {
        MockMultipartFile bigImage = new MockMultipartFile(
                "imageFiles", "room.jpg", "image/jpeg", new byte[10 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> fileStorageService.storeRoomImage(bigImage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vượt quá 10MB");
    }
}
