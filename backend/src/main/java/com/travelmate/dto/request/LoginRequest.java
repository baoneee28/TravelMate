package com.travelmate.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho request dang nhap.
 *
 * Client gui POST /api/auth/login voi body JSON:
 * {
 *   "email": "a@example.com",
 *   "password": "123456"
 * }
 *
 * He thong se:
 * 1. Tim user theo email
 * 2. So sanh password (da ma hoa) voi password client gui len
 * 3. Neu dung: tra ve thong tin user
 * 4. Neu sai: tra ve loi 401 Unauthorized
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * Email dang nhap.
     */
    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong dung dinh dang")
    private String email;

    /**
     * Mat khau.
     */
    @NotBlank(message = "Mat khau khong duoc de trong")
    private String password;
}
