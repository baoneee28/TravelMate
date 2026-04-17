package com.travelmate.exception;

/**
 * Exception tu dinh nghia - nem khi khong tim thay tai nguyen.
 *
 * Vi du su dung:
 * - Tim user theo id nhung khong co trong DB
 * - Tim listing theo id nhung khong ton tai
 * - Tim booking theo id nhung khong co
 *
 * Khi nem exception nay, GlobalExceptionHandler se bat
 * va tra ve HTTP 404 Not Found voi message ro rang.
 *
 * RuntimeException: la unchecked exception, khong can try-catch o cho goi.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Tao exception voi thong bao tuy chinh.
     * Vi du: throw new ResourceNotFoundException("Khong tim thay user voi id: 5")
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
