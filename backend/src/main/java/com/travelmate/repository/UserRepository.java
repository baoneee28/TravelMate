package com.travelmate.repository;

import com.travelmate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository - Interface thao tac voi bang "users" trong MySQL.
 *
 * Ke thua JpaRepository<User, Long>:
 * - User: entity can thao tac
 * - Long: kieu du lieu cua khoa chinh (id)
 *
 * JpaRepository da cung cap san cac method co ban:
 * - save(): luu hoac cap nhat user
 * - findById(): tim user theo id
 * - findAll(): lay tat ca user
 * - deleteById(): xoa user theo id
 * - count(): dem so luong user
 *
 * Cac method tu dinh nghia o duoi de phuc vu auth (dang ky / dang nhap).
 *
 * @Repository: danh dau day la bean Repository trong Spring
 *   (thuc ra Spring Data JPA tu nhan dien, nhung ghi ro cho de hieu)
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tim user theo email.
     * Dung khi: dang nhap (can lay user de so sanh password).
     *
     * Tra ve Optional<User>:
     * - co gia tri neu tim thay
     * - rong neu khong tim thay
     * (dung Optional giup tranh NullPointerException)
     *
     * Spring Data JPA tu dong tao cau lenh SQL tu ten method:
     * SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Kiem tra email da ton tai trong DB chua.
     * Dung khi: dang ky (neu email da co thi khong cho dang ky nua).
     *
     * SQL tuong duong:
     * SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);
}
