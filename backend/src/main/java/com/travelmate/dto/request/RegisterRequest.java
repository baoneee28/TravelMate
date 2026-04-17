package com.travelmate.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO (Data Transfer Object) cho request dang ky tai khoan moi.
 *
 * Tai sao dung DTO thay vi nhan entity truc tiep?
 * 1. Bao mat: khong lo entity voi cac field nhay cam ra ngoai
 * 2. Validate: kiem tra du lieu dau vao tai day truoc khi xu ly
 * 3. Linh hoat: DTO co the khac entity (vi du: khong can field id, createdAt...)
 * 4. Ro rang: biet chinh xac client gui len nhung gi
 *
 * Client gui POST /api/auth/register voi body JSON:
 * {
 *   "fullName": "Nguyen Van A",
 *   "email": "a@example.com",
 *   "password": "123456",
 *   "phone": "0901234567"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * Ho ten nguoi dung.
     * - Bat buoc nhap
     * - Toi da 100 ky tu
     */
    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(max = 100, message = "Ho ten toi da 100 ky tu")
    private String fullName;

    /**
     * Email dung de dang nhap.
     * - Bat buoc nhap
     * - Phai dung dinh dang email
     */
    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong dung dinh dang")
    private String email;

    /**
     * Mat khau.
     * - Bat buoc nhap
     * - It nhat 6 ky tu de dam bao bao mat co ban
     */
    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 6, message = "Mat khau phai co it nhat 6 ky tu")
    private String password;

    /**
     * So dien thoai (khong bat buoc).
     */
    @Size(max = 20, message = "So dien thoai toi da 20 ky tu")
    private String phone;
}
