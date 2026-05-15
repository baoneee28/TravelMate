package com.travelmate.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * FileStorageService - Lưu file ảnh upload từ partner vào thư mục static.
 * Trả về URL tương đối dùng trong <img src="">.
 */
@Service
public class FileStorageService {

    // Thư mục lưu ảnh, tương đối với classpath static resources
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/accommodations/";

    /**
     * Lưu file ảnh thumbnail và trả về URL dùng trong HTML.
     * @param file MultipartFile từ form upload
     * @return URL dạng /uploads/accommodations/xxx.jpg
     * @throws IOException nếu lưu file thất bại
     */
    public String storeThumbnail(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Tạo tên file duy nhất để tránh trùng lặp
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }

        // Chỉ cho phép ảnh
        if (!extension.matches("\\.(jpg|jpeg|png|webp|gif|bmp)")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPG, PNG, WEBP)!");
        }

        String fileName = UUID.randomUUID().toString() + extension;
        Path uploadPath = Paths.get(UPLOAD_DIR);

        // Tạo thư mục nếu chưa có
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        // Trả về URL tương đối để dùng trong HTML
        return "/uploads/accommodations/" + fileName;
    }
}
