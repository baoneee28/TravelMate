package com.travelmate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO response tra ve sau khi dang ky hoac dang nhap thanh cong.
 *
 * Chua thong tin co ban cua user de client hien thi.
 * KHONG BAO GIO tra ve password trong response.
 *
 * Vi du response JSON:
 * {
 *   "id": 1,
 *   "fullName": "Nguyen Van A",
 *   "email": "a@example.com",
 *   "phone": "0901234567",
 *   "role": "USER",
 *   "message": "Dang ky thanh cong"
 * }
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class AuthResponse {

    private Long id;            // ID cua user
    private String fullName;    // Ho ten
    private String email;       // Email
    private String phone;       // So dien thoai
    private String role;        // Vai tro (USER / ADMIN / PARTNER)
    private String message;     // Thong bao ket qua (dang ky / dang nhap thanh cong)
}
