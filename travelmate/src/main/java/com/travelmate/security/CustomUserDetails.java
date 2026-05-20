package com.travelmate.security;

import com.travelmate.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * CustomUserDetails — Wrapper của User entity implement interface UserDetails.
 *
 * Lý do tạo class này:
 *   - Spring Security mặc định chỉ lưu username (email) và role.
 *   - Chúng ta cần thêm fullName để hiển thị trong header sau khi đăng nhập.
 *   - Trong Thymeleaf template, dùng: sec:authentication="principal.fullName"
 *
 * Cách dùng trong template:
 *   <span sec:authentication="principal.fullName">User</span>
 */
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    /**
     * Trả về họ tên đầy đủ của user — dùng để hiển thị trong header.
     * Trong Thymeleaf: sec:authentication="principal.fullName"
     */
    public String getFullName() {
        return user.getName();
    }

    /**
     * Trả về ID của user — tiện dụng khi cần ở controller.
     */
    public Long getId() {
        return user.getId();
    }

    /**
     * Trả về email — cũng chính là username đăng nhập.
     */
    public String getEmail() {
        return user.getEmail();
    }

    /**
     * Trả về số điện thoại — dùng để pre-fill form đặt phòng.
     */
    public String getPhone() {
        return user.getPhone();
    }

    public String getAvatarUrl() {
        return user.getAvatarUrl();
    }

    // ===== Implement UserDetails interface =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * username trong Spring Security = email của user.
     */
    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    /**
     * Chỉ cho phép đăng nhập nếu tài khoản đang ACTIVE.
     */
    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(user.getStatus());
    }
}
