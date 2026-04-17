package com.travelmate.service;

import com.travelmate.dto.request.LoginRequest;
import com.travelmate.dto.request.RegisterRequest;
import com.travelmate.dto.response.AuthResponse;

/**
 * AuthService - Interface dinh nghia cac nghiep vu xac thuc (authentication).
 *
 * Tai sao dung interface + impl?
 * 1. Tach biet "dinh nghia" va "thuc hien":
 *    - Interface noi "can lam gi"
 *    - Impl noi "lam nhu the nao"
 * 2. De test: co the tao mock implementation khi viet unit test
 * 3. De mo rong: sau nay neu doi cach xac thuc (vi du: them OAuth, JWT...)
 *    chi can tao impl moi, khong can sua code cu
 * 4. Chuyen nghiep: day la pattern pho bien trong Spring Boot
 */
public interface AuthService {

    /**
     * Dang ky tai khoan moi.
     *
     * Luong xu ly:
     * 1. Kiem tra email da ton tai chua
     * 2. Ma hoa password bang BCrypt
     * 3. Tao user moi voi role = USER, status = ACTIVE
     * 4. Luu vao database
     * 5. Tra ve thong tin user (khong co password)
     *
     * @param request du lieu dang ky tu client
     * @return AuthResponse chua thong tin user vua tao
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Dang nhap.
     *
     * Luong xu ly:
     * 1. Tim user theo email
     * 2. So sanh password client gui voi password da ma hoa trong DB
     * 3. Neu dung: tra ve thong tin user
     * 4. Neu sai: nem UnauthorizedException
     *
     * @param request du lieu dang nhap (email + password)
     * @return AuthResponse chua thong tin user
     */
    AuthResponse login(LoginRequest request);
}
