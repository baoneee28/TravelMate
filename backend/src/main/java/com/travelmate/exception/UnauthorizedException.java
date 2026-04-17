package com.travelmate.exception;

/**
 * Exception tu dinh nghia - nem khi nguoi dung chua xac thuc hoac xac thuc sai.
 *
 * Vi du su dung:
 * - Dang nhap sai email hoac password
 * - Goi API can dang nhap nhung chua dang nhap
 * - Token het han hoac khong hop le (khi co JWT sau nay)
 *
 * Khi nem exception nay, GlobalExceptionHandler se bat
 * va tra ve HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
