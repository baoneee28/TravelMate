package com.travelmate.exception;

/**
 * Exception tu dinh nghia - nem khi request khong hop le.
 *
 * Vi du su dung:
 * - Dang ky voi email da ton tai
 * - Gui du lieu thieu field bat buoc
 * - Gia tri khong hop le (vi du: so khach am)
 *
 * Khi nem exception nay, GlobalExceptionHandler se bat
 * va tra ve HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
