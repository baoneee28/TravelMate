package com.travelmate.config;

import com.travelmate.security.CustomLoginSuccessHandler;
import com.travelmate.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - Cau hinh bao mat cho TravelMate (MVC + Thymeleaf).
 *
 * === THAY DOI QUAN TRONG SO VOI PHIEN BAN CU ===
 * TRUOC (REST API thuần):
 *   - CSRF tat hoan toan
 *   - Session STATELESS (khong luu session)
 *   - Moi request phai gui token
 *
 * SAU (MVC + Thymeleaf):
 *   - CSRF BAT cho form HTML (bao ve chong tan cong CSRF)
 *   - CSRF TAT cho /api/** (API van can goi tu Postman/AJAX)
 *   - Session DUNG (luu trang thai dang nhap tren server)
 *   - Form login: Spring Security tu xu ly dang nhap qua form
 *   - Phan quyen theo role cho tung nhom URL
 *
 * Annotation:
 * - @Configuration: class cau hinh Spring
 * - @EnableWebSecurity: bat bao mat web
 * - @RequiredArgsConstructor: inject CustomUserDetailsService tu dong
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomLoginSuccessHandler loginSuccessHandler;

    /**
     * Cau hinh chuoi bo loc bao mat (Security Filter Chain).
     *
     * Day la NOI QUAN TRONG NHAT cua bao mat:
     * - Quyet dinh URL nao ai duoc truy cap
     * - Cau hinh cach dang nhap / dang xuat
     * - Bao ve chong CSRF
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ============================================================
                // CSRF (Cross-Site Request Forgery)
                // - BAT cho form HTML (Thymeleaf tu dong them CSRF token vao form)
                // - TAT cho /api/** de Postman va AJAX van goi duoc
                // ============================================================
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )

                // ============================================================
                // PHAN QUYEN TRUY CAP (Authorization)
                // Quy dinh URL nao can role gi de truy cap
                // ============================================================
                .authorizeHttpRequests(auth -> auth
                        // --- STATIC ASSETS: ai cung truy cap duoc ---
                        // CSS, JS, Images can load duoc khi chua dang nhap
                        .requestMatchers("/assets/**").permitAll()

                        // --- TRANG PUBLIC: ai cung xem duoc ---
                        .requestMatchers(
                                "/",              // Trang chu
                                "/login",         // Trang dang nhap
                                "/register",      // Trang dang ky
                                "/travel",        // Goi y du lich
                                "/news",          // Tin tuc
                                "/contact",       // Lien he
                                "/accommodations/**"  // Danh sach noi luu tru
                        ).permitAll()

                        // --- API AUTH: ai cung goi duoc (dang ky, dang nhap) ---
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/test/**").permitAll()

                        // --- API ADMIN: chi role ADMIN moi goi duoc ---
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // --- ADMIN: chi role ADMIN moi vao duoc ---
                        // hasRole("ADMIN") tuong duong hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // --- PARTNER: chi role PARTNER moi vao duoc ---
                        .requestMatchers("/partner/**").hasRole("PARTNER")

                        // --- USER pages: chi USER va ADMIN moi duoc truy cap ---
                        // hasAnyRole("USER", "ADMIN"): ADMIN can xem trang user de ho tro/kiem tra
                        // PARTNER se bi chan vi ho chi quan ly listing, khong phai nguoi dat phong
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")

                        // --- API BOOKING: chi USER moi duoc tao/huy booking ---
                        // - PARTNER khong dat phong (ho la nha cung cap, khong phai khach)
                        // - ADMIN quan ly qua /admin/**, khong dung endpoint nay
                        // - GET /api/bookings/availability: cho phep moi authenticated user (xem lich)
                        .requestMatchers(
                                org.springframework.http.HttpMethod.POST,
                                "/api/bookings", "/api/bookings/*/cancel"
                        ).hasRole("USER")
                        .requestMatchers("/api/bookings/**").authenticated()

                        // --- MOI THU KHAC: phai dang nhap ---
                        .anyRequest().authenticated()
                )

                // ============================================================
                // FORM LOGIN (Spring Security tu xu ly)
                //
                // Cach hoat dong:
                // 1. User truy cap trang can dang nhap (vd: /user/mybooking)
                // 2. Spring redirect ve /login
                // 3. User nhap email + password, submit form
                // 4. Spring Security nhan POST /login, goi UserDetailsService
                // 5. Neu dung -> redirect ve trang ban dau hoac defaultSuccessUrl
                // 6. Neu sai -> redirect ve /login?error=true
                //
                // usernameParameter("email"): vi form login dung field "email"
                //   (mac dinh Spring dung "username", minh doi thanh "email")
                // ============================================================
                .formLogin(form -> form
                        .loginPage("/login")              // Trang login TUY CHINH (khong dung trang mac dinh cua Spring)
                        .loginProcessingUrl("/login")     // URL xu ly POST form login
                        .usernameParameter("email")       // Ten field email trong form HTML
                        .passwordParameter("password")    // Ten field password trong form HTML
                        .successHandler(loginSuccessHandler) // Redirect theo role: ADMIN->/admin/dashboard, PARTNER->/partner/dashboard, USER->/
                        .failureUrl("/login?error=true")
                        .permitAll()                      // Ai cung truy cap duoc trang login
                )

                // ============================================================
                // LOGOUT
                // Khi user bam "Dang xuat":
                // 1. Spring xoa session
                // 2. Redirect ve /login?logout=true
                // ============================================================
                .logout(logout -> logout
                        .logoutUrl("/logout")                  // URL de dang xuat
                        .logoutSuccessUrl("/login?logout=true") // Sau khi xuat -> ve trang login
                        .invalidateHttpSession(true)           // Xoa session
                        .deleteCookies("JSESSIONID")           // Xoa cookie
                        .permitAll()
                )

                // ============================================================
                // USER DETAILS SERVICE
                // Chi cho Spring Security biet dung CustomUserDetailsService
                // de tim user trong database khi dang nhap
                // ============================================================
                .userDetailsService(userDetailsService);

        return http.build();
    }

    /**
     * PasswordEncoder - Ma hoa mat khau bang BCrypt.
     *
     * BCrypt la giai thuat ma hoa 1 chieu:
     * - "123456" -> "$2a$10$..." (khong the giai nguoc)
     * - Moi lan ma hoa cho ket qua khac nhau (vi co salt)
     * - An toan cho production
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager - Quan ly xac thuc.
     * Can cho truong hop dang nhap bang API (AuthApiController).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
