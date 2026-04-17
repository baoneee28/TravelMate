package com.travelmate.exception;

import com.travelmate.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - Xu ly tat ca exception trong toan bo ung dung.
 *
 * Tai sao can lam nay?
 * - Khong co handler nay, khi co loi Spring Boot se tra ve trang HTML mac dinh,
 *   hoac tra JSON voi format khong nhat quan
 * - Voi handler nay, MOI loi deu tra ve cung format ApiResponse,
 *   sach se, de hieu, chuyen nghiep
 *
 * @RestControllerAdvice: danh dau day la handler toan cuc (global),
 *   tu dong bat exception tu tat ca controller.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xu ly khi khong tim thay tai nguyen.
     * Tra ve HTTP 404 Not Found.
     *
     * Vi du: Tim user theo id = 999 nhung khong co trong DB
     * Response: { "success": false, "message": "Khong tim thay user voi id: 999" }
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Xu ly khi request khong hop le.
     * Tra ve HTTP 400 Bad Request.
     *
     * Vi du: Dang ky voi email da ton tai
     * Response: { "success": false, "message": "Email da ton tai" }
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Xu ly khi xac thuc that bai.
     * Tra ve HTTP 401 Unauthorized.
     *
     * Vi du: Dang nhap sai password
     * Response: { "success": false, "message": "Email hoac mat khau khong dung" }
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Xu ly loi validation (khi dung @Valid tren DTO).
     * Tra ve HTTP 400 Bad Request kem chi tiet loi tung field.
     *
     * Vi du: Dang ky voi email rong va password qua ngan
     * Response: {
     *   "success": false,
     *   "message": "Du lieu khong hop le",
     *   "data": {
     *     "email": "Email khong duoc de trong",
     *     "password": "Mat khau phai co it nhat 6 ky tu"
     *   }
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        // Lay tat ca loi validation va dua vao map
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Du lieu khong hop le")
                        .data(errors)
                        .build());
    }

    /**
     * Xu ly tat ca cac exception khong duoc xu ly rieng o tren.
     * Day la "luoi an toan cuoi cung" de khong bi lo loi ra ngoai.
     * Tra ve HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Loi he thong: " + ex.getMessage()));
    }
}
