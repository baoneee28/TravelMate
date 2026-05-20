package com.travelmate.service;

import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * UserService — Xử lý nghiệp vụ liên quan đến tài khoản người dùng.
 *
 * Tách riêng logic khỏi Controller để dễ test và bảo trì.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Kiểm tra email đã tồn tại chưa.
     */
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Khóa tài khoản người dùng. Không cho phép khóa Admin.
     */
    public User lockUser(Long id) {
        User user = userRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại!"));
        if (user.getRole() == User.Role.ADMIN)
            throw new IllegalArgumentException("Không thể khóa tài khoản Admin!");
        user.setStatus("LOCKED");
        return userRepository.save(user);
    }

    /**
     * Mở khóa tài khoản người dùng.
     */
    public User unlockUser(Long id) {
        User user = userRepository.findById(java.util.Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại!"));
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    /**
     * Đăng ký tài khoản mới.
     * - Mã hóa mật khẩu bằng BCrypt trước khi lưu
     * - Role mặc định: USER
     * - Status mặc định: ACTIVE
     */
    public User register(String firstName, String lastName, String email, String phone, String rawPassword) {
        String fullName = firstName.trim() + " " + lastName.trim();

        User user = new User();
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setName(fullName);
        user.setShortName(lastName.trim());
        user.setPhone(phone != null ? phone.trim() : "");
        user.setRole(User.Role.USER);
        user.setStatus("ACTIVE");

        return userRepository.save(user);
    }
}
