package com.travelmate.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * ApiResponse<T> - DTO chuan hoa tat ca response cua API.
 *
 * Muc dich: moi API trong TravelMate deu tra ve cung 1 format,
 * giup frontend de xu ly va hien thi.
 *
 * Format chuan:
 * {
 *   "success": true,          // API thanh cong hay that bai
 *   "message": "Thanh cong",  // Thong bao cho nguoi dung
 *   "data": { ... }           // Du lieu tra ve (co the null neu khong can)
 * }
 *
 * Vi du thanh cong:
 * {
 *   "success": true,
 *   "message": "Dang ky thanh cong",
 *   "data": { "id": 1, "email": "a@example.com" }
 * }
 *
 * Vi du that bai:
 * {
 *   "success": false,
 *   "message": "Email da ton tai",
 *   "data": null
 * }
 *
 * @JsonInclude(NON_NULL): neu field nao null thi khong hien trong JSON
 *   (giam kich thuoc response, gon gang hon)
 *
 * Generic <T>: kieu du lieu cua field "data" co the la bat ky kieu nao.
 *   Vi du: ApiResponse<AuthResponse>, ApiResponse<List<User>>...
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;    // true = thanh cong, false = that bai
    private String message;     // Thong bao cho nguoi dung
    private T data;             // Du lieu tra ve (generic)

    /**
     * Tao response thanh cong kem du lieu.
     * Vi du: ApiResponse.success("Dang ky thanh cong", authResponse)
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Tao response thanh cong chi co thong bao, khong co du lieu.
     * Vi du: ApiResponse.success("Xoa thanh cong")
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Tao response that bai.
     * Vi du: ApiResponse.error("Email da ton tai")
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }
}
