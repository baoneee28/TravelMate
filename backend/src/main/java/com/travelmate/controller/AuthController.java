package com.travelmate.controller;

import com.travelmate.dto.request.LoginRequest;
import com.travelmate.dto.request.RegisterRequest;
import com.travelmate.dto.response.ApiResponse;
import com.travelmate.dto.response.AuthResponse;
import com.travelmate.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - Controller xu ly cac API lien quan den xac thuc.
 *
 * Cac API:
 * - POST /api/auth/register : dang ky tai khoan moi
 * - POST /api/auth/login    : dang nhap
 *
 * Tat ca cac API nay deu o trang thai "permitAll" trong SecurityConfig,
 * nghia la ai cung goi duoc ma khong can dang nhap.
 *
 * Annotation giai thich:
 * - @RestController: day la REST controller, response tu dong chuyen thanh JSON
 * - @RequestMapping("/api/auth"): tat ca API ben trong bat dau bang /api/auth
 * - @RequiredArgsConstructor: Lombok inject dependency qua constructor
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /**
     * AuthService xu ly nghiep vu, controller chi lam nhiem vu:
     * 1. Nhan request tu client
     * 2. Validate du lieu (@Valid)
     * 3. Goi service xu ly
     * 4. Tra response ve client
     *
     * Controller KHONG nen chua logic nghiep vu phuc tap.
     */
    private final AuthService authService;

    /**
     * API Dang ky tai khoan moi.
     *
     * Method: POST
     * URL: /api/auth/register
     *
     * Body JSON:
     * {
     *   "fullName": "Nguyen Van A",
     *   "email": "a@example.com",
     *   "password": "123456",
     *   "phone": "0901234567"
     * }
     *
     * Response thanh cong (HTTP 201):
     * {
     *   "success": true,
     *   "message": "Dang ky thanh cong",
     *   "data": { "id": 1, "fullName": "Nguyen Van A", ... }
     * }
     *
     * @Valid: tu dong validate du lieu theo annotation trong RegisterRequest
     *   (vi du: @NotBlank, @Email, @Size...)
     *   Neu validate fail -> MethodArgumentNotValidException -> GlobalExceptionHandler xu ly
     *
     * @RequestBody: chuyen JSON body thanh object RegisterRequest
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED) // HTTP 201: tao thanh cong
                .body(ApiResponse.success("Dang ky thanh cong", response));
    }

    /**
     * API Dang nhap.
     *
     * Method: POST
     * URL: /api/auth/login
     *
     * Body JSON:
     * {
     *   "email": "a@example.com",
     *   "password": "123456"
     * }
     *
     * Response thanh cong (HTTP 200):
     * {
     *   "success": true,
     *   "message": "Dang nhap thanh cong",
     *   "data": { "id": 1, "fullName": "Nguyen Van A", "role": "USER", ... }
     * }
     *
     * Response that bai (HTTP 401):
     * {
     *   "success": false,
     *   "message": "Email hoac mat khau khong dung"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Dang nhap thanh cong", response));
    }
}
