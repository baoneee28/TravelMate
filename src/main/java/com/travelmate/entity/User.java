package com.travelmate.entity;

import com.travelmate.enums.Role;
import com.travelmate.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entity User - anh xa voi bang "users" trong MySQL.
 *
 * Day la entity quan trong nhat cua he thong TravelMate vi:
 * - Moi module deu phu thuoc User (booking, review, listing, payment...)
 * - Role quyet dinh luong nghiep vu (USER dat cho, PARTNER tao listing, ADMIN duyet)
 * - Moi nguoi dung deu phai co tai khoan
 *
 * Ke thua BaseEntity de co san: id, createdAt, updatedAt
 *
 * Annotation giai thich:
 * - @Entity: danh dau class nay la entity JPA, se anh xa vao bang trong DB
 * - @Table(name = "users"): ten bang trong MySQL la "users"
 *   (khong dung "user" vi "user" la tu khoa cua MySQL)
 * - @Getter/@Setter: Lombok tu dong tao getter/setter
 * - @NoArgsConstructor: Lombok tao constructor khong tham so (JPA bat buoc can)
 * - @AllArgsConstructor: Lombok tao constructor day du tham so
 * - @Builder: Lombok tao builder pattern, giup tao object de dang
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    /**
     * Ho ten day du cua nguoi dung.
     * - @NotBlank: khong duoc de trong
     * - @Size(max = 100): toi da 100 ky tu
     * - @Column(nullable = false): cot nay khong duoc NULL trong DB
     */
    @NotBlank(message = "Ho ten khong duoc de trong")
    @Size(max = 100, message = "Ho ten toi da 100 ky tu")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * Email cua nguoi dung - dung de dang nhap.
     * - unique = true: khong cho phep 2 user cung email
     * - @Email: phai dung dinh dang email
     */
    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong dung dinh dang")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Mat khau da duoc ma hoa (BCrypt).
     * KHONG BAO GIO luu mat khau goc (plain text) vao database.
     */
    @NotBlank(message = "Mat khau khong duoc de trong")
    @Column(nullable = false)
    private String password;

    /**
     * So dien thoai cua nguoi dung.
     * Co the null (khong bat buoc nhap khi dang ky).
     */
    @Size(max = 20, message = "So dien thoai toi da 20 ky tu")
    @Column(length = 20)
    private String phone;

    /**
     * Vai tro cua nguoi dung trong he thong.
     * - Dung enum Role (USER, ADMIN, PARTNER)
     * - @Enumerated(STRING): luu ten enum duoi dang chuoi trong DB
     *   (vi du: "USER", "ADMIN") thay vi so (0, 1, 2)
     *   giup doc DB de hieu hon
     * - Mac dinh la USER khi dang ky
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * Trang thai tai khoan.
     * - Dung enum UserStatus (ACTIVE, INACTIVE, BLOCKED)
     * - Mac dinh la ACTIVE khi dang ky
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;
}
