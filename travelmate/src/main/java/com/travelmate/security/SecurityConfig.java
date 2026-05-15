package com.travelmate.security;

import com.travelmate.service.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — Cấu hình bảo mật Spring Security cho TravelMate.
 *
 * Luồng hoạt động:
 *   1. User vào form login /auth/login → nhập email + password
 *   2. Spring Security gọi UserDetailsServiceImpl.loadUserByUsername(email)
 *   3. So sánh password nhập vào (BCrypt) với password trong DB
 *   4. Nếu đúng → gọi CustomAuthSuccessHandler để redirect theo role
 *   5. Nếu sai → redirect về /auth/login?error
 *
 * Phân quyền route hiện tại:
 *   - /admin/**  → chỉ ADMIN (sẽ bật sau khi demo ổn định)
 *   - /partner/** → chỉ PARTNER (sẽ bật sau khi demo ổn định)
 *   - Còn lại → public tạm thời để tiện test
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Inject UserDetailsService để Spring Security biết cách load user từ DB
    private final UserDetailsServiceImpl userDetailsService;

    // Inject handler redirect theo role sau khi login thành công
    private final CustomAuthSuccessHandler successHandler;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          CustomAuthSuccessHandler successHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
    }

    /**
     * BCryptPasswordEncoder — Bean mã hóa mật khẩu bằng BCrypt.
     *
     * BCrypt là chuẩn mã hóa password phổ biến nhất hiện nay:
     *   - Tự động thêm "salt" ngẫu nhiên → cùng password mỗi lần hash ra kết quả khác nhau
     *   - Chống rainbow table attack
     *   - KHÔNG bao giờ lưu plain text password trong DB
     *
     * Dùng @Bean để Spring có thể inject vào các chỗ cần (như khi insert user mẫu).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * DaoAuthenticationProvider — Kết nối UserDetailsService với PasswordEncoder.
     *
     * Provider này làm 2 việc:
     *   1. Gọi userDetailsService.loadUserByUsername(email) để lấy thông tin user từ DB
     *   2. Dùng BCrypt để so sánh password nhập vào với password trong DB
     */
    @SuppressWarnings("deprecation")
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);   // đọc user từ DB
        provider.setPasswordEncoder(passwordEncoder());       // so sánh BCrypt password
        return provider;
    }

    /**
     * SecurityFilterChain — Định nghĩa toàn bộ rule bảo mật.
     *
     * Đây là điểm trung tâm cấu hình Spring Security.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF tạm thời để dễ test REST API.
            // Khi production: cần bật lại và thêm CSRF token vào form.
            .csrf(csrf -> csrf.disable())

            // Đăng ký AuthenticationProvider vừa tạo
            .authenticationProvider(authenticationProvider())

            // Cấu hình phân quyền route
            .authorizeHttpRequests(auth -> auth

                // Static resources — CSS, JS, images phải public
                // Nếu không permitAll thì CSS/JS sẽ bị chặn → trang trắng
                .requestMatchers("/assets/**").permitAll()

                // Trang auth — đăng nhập/đăng ký phải public
                .requestMatchers("/auth/**").permitAll()

                // Trang chủ và các trang user — public, không cần đăng nhập
                // /accommodations (danh sách) và /accommodations/{id} (chi tiết) đều public
                // Chỉ /booking mới yêu cầu đăng nhập (cấu hình bên dưới)
                // /error phải public để Spring Boot error page hiển thị được khi có exception
                .requestMatchers("/", "/accommodations", "/accommodations/**", "/travel", "/news",
                                 "/contact", "/contact/submit", "/vouchers", "/error").permitAll()

                // Booking routes — chỉ USER role mới được đặt phòng / xem booking của mình
                // ADMIN và PARTNER không cần dùng luồng này
                .requestMatchers("/booking", "/booking/**", "/my-bookings", "/my-bookings/**")
                    .hasRole("USER")

                // Admin routes — chỉ tài khoản có role ADMIN mới vào được
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Partner routes — chỉ tài khoản có role PARTNER mới vào được
                .requestMatchers("/partner/**").hasRole("PARTNER")

                // Các route còn lại phải đăng nhập — không để public hoàn toàn.
                // Nếu có trang public mới cần thêm, hãy thêm vào requestMatchers().permitAll() ở trên.
                .anyRequest().authenticated()
            )

            // Cấu hình Form Login
            .formLogin(form -> form
                .loginPage("/auth/login")                  // trang đăng nhập tùy chỉnh
                .loginProcessingUrl("/auth/login")         // URL nhận POST khi submit form
                .usernameParameter("email")                // tên field email trong form HTML
                .passwordParameter("password")             // tên field password trong form HTML
                .successHandler(successHandler)            // redirect theo role sau login thành công
                .failureUrl("/auth/login?error=true")      // redirect về đây khi sai email/password
                .permitAll()
            )

            // Cấu hình Logout
            .logout(logout -> logout
                .logoutUrl("/auth/logout")                 // URL để logout (GET hoặc POST)
                .logoutSuccessUrl("/auth/login?logout")    // về trang login sau khi logout
                .invalidateHttpSession(true)               // xóa session
                .clearAuthentication(true)                 // xóa thông tin xác thực
                .permitAll()
            );

        return http.build();
    }
}
