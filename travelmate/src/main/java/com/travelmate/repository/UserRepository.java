package com.travelmate.repository;

import com.travelmate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository — Giao tiếp với bảng `users` trong MySQL.
 *
 * Kế thừa JpaRepository nên tự động có các method cơ bản:
 *   - save(), findById(), findAll(), deleteById()...
 *
 * Chúng ta chỉ cần thêm method tìm theo email (dùng cho đăng nhập).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tìm user theo email — Spring Data JPA tự generate SQL:
     * SELECT * FROM users WHERE email = ?
     *
     * Trả về Optional để tránh NullPointerException:
     *   - Optional.of(user) nếu tìm thấy
     *   - Optional.empty() nếu không có user với email đó
     */
    Optional<User> findByEmail(String email);
}
