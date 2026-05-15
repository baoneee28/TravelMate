package com.travelmate.entity;

import com.travelmate.entity.enums.PropertyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User — Entity ánh xạ bảng `users` trong MySQL.
 *
 * Đây là bảng trung tâm của hệ thống xác thực.
 * Spring Security sẽ đọc thông tin từ bảng này để xác thực đăng nhập.
 *
 * 3 role trong TravelMate:
 *   - USER    : người dùng đặt phòng
 *   - ADMIN   : quản trị viên hệ thống
 *   - PARTNER : đối tác cho thuê lưu trú
 *
 * Schema bảng users trong DB (đọc từ MySQL):
 *   id, created_at, updated_at, email, full_name, password, phone, role, status, name
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * Khóa chính — tự động tăng (AUTO_INCREMENT trong MySQL).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Email — dùng làm username để đăng nhập.
     * Phải unique: không cho 2 người dùng cùng email.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Mật khẩu đã được mã hóa bằng BCrypt.
     * KHÔNG bao giờ lưu plain text.
     */
    @Column(nullable = false)
    private String password;

    /**
     * Tên hiển thị — ánh xạ cột full_name trong DB.
     */
    @Column(name = "full_name", nullable = false, length = 100)
    private String name;

    /**
     * Tên ngắn — ánh xạ cột name trong DB (bảng cũ có cả 2 cột name và full_name).
     * Đặt insertable = false để tránh lỗi khi insert (dùng full_name là chính).
     */
    @Column(name = "name", nullable = false, length = 100)
    private String shortName;

    /**
     * Số điện thoại (tùy chọn).
     */
    @Column(length = 20)
    private String phone;

    /**
     * Role của user — lưu dưới dạng String trong DB (EnumType.STRING).
     * Ví dụ: "ADMIN", "USER", "PARTNER"
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * Trạng thái tài khoản (ACTIVE, INACTIVE, BANNED...).
     * Mặc định: ACTIVE khi tạo mới.
     */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    /**
     * Loại hình lưu trú Partner đăng ký quản lý.
     * Business rule: 1 Partner chỉ quản lý 1 loại lưu trú.
     * VD: HOTEL → partner chỉ tạo được Hotel.
     * null → chưa đăng ký / không phải partner.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "partner_property_type", length = 20)
    private PropertyType partnerPropertyType;

    /**
     * Thông tin ngân hàng nhận quyết toán — chỉ dùng cho PARTNER.
     * Không xử lý giao dịch thật, chỉ lưu để Admin biết chuyển khoản cho ai.
     */
    @Column(name = "bank_account_number", length = 30)
    private String bankAccountNumber;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_holder", length = 100)
    private String bankAccountHolder;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;


    /**
     * Thời gian tạo tài khoản — tự động gán khi INSERT.
     * updatable = false: không cho phép cập nhật lại.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời gian cập nhật lần cuối — tự động cập nhật khi UPDATE.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Enum định nghĩa các role hợp lệ trong TravelMate.
     * Luật: chỉ có đúng 3 role này, không được thêm ngoài scope.
     */
    public enum Role {
        USER,       // người dùng đặt phòng
        ADMIN,      // quản trị viên hệ thống
        PARTNER     // đối tác cho thuê lưu trú
    }
}
