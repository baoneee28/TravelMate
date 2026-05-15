package com.travelmate.service;

import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import com.travelmate.security.CustomUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsServiceImpl — "Cầu nối" giữa Spring Security và database của chúng ta.
 *
 * Khi user nhập email + password vào form đăng nhập:
 *   1. Spring Security gọi loadUserByUsername(email)
 *   2. Method này tìm user trong DB theo email
 *   3. Trả về CustomUserDetails (chứa email, password đã mã hóa, role, và fullName)
 *   4. Spring Security tự so sánh password nhập vào với password trong DB
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Spring Security gọi method này khi user submit form login.
     *
     * @param email — email user nhập vào
     * @return CustomUserDetails — expose thêm getFullName() để Thymeleaf dùng
     * @throws UsernameNotFoundException — nếu không tìm thấy email trong DB
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản với email: " + email
                ));

        // Trả về CustomUserDetails thay vì Spring's built-in User
        // → Thymeleaf có thể dùng: sec:authentication="principal.fullName"
        return new CustomUserDetails(user);
    }
}
