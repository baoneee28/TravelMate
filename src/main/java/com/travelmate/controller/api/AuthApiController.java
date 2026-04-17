package com.travelmate.controller.api;

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
 * AuthApiController - REST API xu ly xac thuc (dang ky, dang nhap).
 *
 * === QUAN TRONG: THONG NHAT LOGIN FLOW ===
 * TravelMate da chot dung Spring Security form login + session la CHINH.
 * Nghia la:
 *   - Dang nhap CHINH: form HTML tai /login (POST /login do Spring Security xu ly)
 *   - Dang nhap PHU: API /api/auth/login (chi dung cho AJAX, Postman, test)
 *
 * Khi nao dung API nay:
 *   1. Test bang Postman trong qua trinh phat trien
 *   2. AJAX login dac biet (neu frontend can login khong reload trang)
 *   3. Ho tro mobile app sau nay (neu co)
 *
 * Khi nao KHONG dung API nay:
 *   - KHONG dung lam login chinh cho trang web
 *   - KHONG code them login logic moi vao day
 *   - Moi thay doi login phai sua o SecurityConfig + form login truoc
 *
 * API dang ky (/api/auth/register) van dung binh thuong vi form register
 * gui du lieu bang fetch/AJAX len endpoint nay.
 *
 * Cac API:
 * - POST /api/auth/register : dang ky tai khoan moi
 * - POST /api/auth/login    : dang nhap (BO TRO, khong phai login chinh)
 *
 * Annotation giai thich:
 * - @RestController: response tu dong chuyen thanh JSON
 * - @RequestMapping("/api/auth"): prefix cho tat ca API auth
 * - @RequiredArgsConstructor: Lombok inject dependency qua constructor
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

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
     * @RequestBody: chuyen JSON body thanh object RegisterRequest
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
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
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Dang nhap thanh cong", response));
    }
}
