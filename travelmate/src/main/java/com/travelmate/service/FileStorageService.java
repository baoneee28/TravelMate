package com.travelmate.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".webp", "image/webp"
    );

    /**
     * Resolve thư mục upload dựa trên đường dẫn tuyệt đối của classpath static,
     * tránh lỗi khi working directory là Tomcat temp dir (VS Code extension).
     */
    private Path resolveUploadDir(String subDir) throws IOException {
        Path staticDir = Paths.get(new ClassPathResource("static").getFile().getAbsolutePath());
        Path dir = staticDir.resolve("uploads").resolve(subDir);
        Files.createDirectories(dir);
        return dir;
    }

    private String extractExtension(String originalName) {
        if (originalName != null && originalName.contains(".")) {
            return originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        return "";
    }

    private void validateImageFile(MultipartFile file) {
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Ảnh vượt quá 10MB. Vui lòng chọn ảnh nhỏ hơn.");
        }
        String extension = extractExtension(file.getOriginalFilename());
        String expectedContentType = ALLOWED_IMAGE_TYPES.get(extension);
        if (expectedContentType == null) {
            throw new IllegalArgumentException("Chỉ hỗ trợ JPG, PNG, WEBP.");
        }
        String actualContentType = file.getContentType();
        if (actualContentType == null || !actualContentType.equalsIgnoreCase(expectedContentType)) {
            throw new IllegalArgumentException("Định dạng ảnh không khớp nội dung file. Vui lòng chọn JPG, PNG hoặc WEBP hợp lệ.");
        }
    }

    public String storeThumbnail(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        validateImageFile(file);
        String extension = extractExtension(file.getOriginalFilename());

        String fileName = UUID.randomUUID() + extension;
        Path uploadDir = resolveUploadDir("accommodations");
        file.transferTo(java.util.Objects.requireNonNull(uploadDir.resolve(fileName).toFile()));
        return "/uploads/accommodations/" + fileName;
    }

    public String storeAvatar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        validateImageFile(file);
        String extension = extractExtension(file.getOriginalFilename());

        String fileName = UUID.randomUUID() + extension;
        Path uploadDir = resolveUploadDir("avatars");
        file.transferTo(java.util.Objects.requireNonNull(uploadDir.resolve(fileName).toFile()));
        return "/uploads/avatars/" + fileName;
    }

    public String storeRoomImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        validateImageFile(file);
        String extension = extractExtension(file.getOriginalFilename());

        String fileName = UUID.randomUUID() + extension;
        Path uploadDir = resolveUploadDir("rooms");
        file.transferTo(java.util.Objects.requireNonNull(uploadDir.resolve(fileName).toFile()));
        return "/uploads/rooms/" + fileName;
    }
}
