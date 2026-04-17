package com.travelmate.security;

import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * CustomUserDetailsService - Lop nay giup Spring Security biet cach
 * lay thong tin user tu database MySQL de xac thuc khi dang nhap.
 *
 * CACH HOAT DONG:
 * 1. User nhap email + password vao form login
 * 2. Spring Security goi loadUserByUsername(email)
 * 3. Method nay query database de tim User theo email
 * 4. Neu tim thay -> tra ve UserDetails (chua email, password da ma hoa, role)
 * 5. Spring Security tu dong so sanh password user nhap voi password trong DB
 * 6. Neu dung -> cho dang nhap, sai -> bao loi
 *
 * TAI SAO CAN:
 * - Spring Security KHONG tu biet database cua minh nhu the nao
 * - Minh phai "day" no cach lay user tu bang "users" cua TravelMate
 * - Day la cau noi giua Spring Security va database MySQL
 *
 * Annotation giai thich:
 * - @Service: danh dau la Spring Bean, Spring tu dong quan ly
 * - @RequiredArgsConstructor: Lombok inject UserRepository qua constructor
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    // Repository de truy van bang "users" trong MySQL
    private final UserRepository userRepository;

    /**
     * Method nay duoc Spring Security goi tu dong khi user dang nhap.
     *
     * @param email - email user nhap vao form login
     *              (o day minh dung email lam "username" de dang nhap)
     * @return UserDetails - object chua thong tin xac thuc
     * @throws UsernameNotFoundException neu khong tim thay email trong DB
     *
     * CHI TIET:
     * - "ROLE_" + role.name(): Spring Security yeu cau role phai co prefix "ROLE_"
     *   Vi du: role = USER -> authority = "ROLE_USER"
     *   Khi dung hasRole("USER") trong SecurityConfig, Spring tu dong them "ROLE_" phia truoc
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Tim user trong database theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Khong tim thay tai khoan voi email: " + email));

        // Chuyen entity User cua minh thanh UserDetails cua Spring Security
        // Spring Security can 3 thong tin chinh:
        // 1. username (email) - de dinh danh
        // 2. password (da ma hoa BCrypt) - de so sanh
        // 3. authorities (role) - de phan quyen
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),       // dung email lam username
                user.getPassword(),    // password da duoc ma hoa BCrypt trong DB
                Collections.singletonList(
                        // Tao authority tu role cua user
                        // Vi du: Role.USER -> "ROLE_USER"
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
    }
}
