package com.travelmate.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

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

    private void validateImageExtension(String extension) {
        if (!extension.matches("\\.(jpg|jpeg|png|webp|gif|bmp)")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPG, PNG, WEBP)!");
        }
    }

    public String storeThumbnail(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String extension = extractExtension(file.getOriginalFilename());
        validateImageExtension(extension);

        String fileName = UUID.randomUUID() + extension;
        Path uploadDir = resolveUploadDir("accommodations");
        file.transferTo(java.util.Objects.requireNonNull(uploadDir.resolve(fileName).toFile()));
        return "/uploads/accommodations/" + fileName;
    }

    public String storeAvatar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        String extension = extractExtension(file.getOriginalFilename());
        validateImageExtension(extension);

        String fileName = UUID.randomUUID() + extension;
        Path uploadDir = resolveUploadDir("avatars");
        file.transferTo(java.util.Objects.requireNonNull(uploadDir.resolve(fileName).toFile()));
        return "/uploads/avatars/" + fileName;
    }
}
