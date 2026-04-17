package com.travelmate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - Cau hinh bao mat cho toan bo ung dung TravelMate.
 *
 * O giai doan dau, cau hinh nay se:
 * 1. Mo tat ca API auth (dang ky, dang nhap) va test cho ai cung goi duoc
 * 2. Cac API khac cung tam thoi mo de tien phat trien
 * 3. Tat CSRF vi day la REST API (khong dung form truyen thong)
 * 4. Dung stateless session (khong luu session tren server)
 * 5. Cung cap PasswordEncoder (BCrypt) de ma hoa mat khau
 *
 * Sau nay khi lam JWT se:
 * - them JwtAuthenticationFilter
 * - chan API theo role (USER, ADMIN, PARTNER)
 * - them cors config cho frontend
 *
 * Annotation giai thich:
 * - @Configuration: danh dau day la class cau hinh Spring
 * - @EnableWebSecurity: bat tinh nang bao mat web cua Spring Security
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Cau hinh chuoi bo loc bao mat (Security Filter Chain).
     *
     * Day la noi quy dinh:
     * - API nao duoc truy cap tu do (permitAll)
     * - API nao can dang nhap (authenticated)
     * - API nao can role cu the (hasRole)
     *
     * Hien tai: MO TAT CA de tien phat trien.
     * Khi lam xong auth + JWT se chan lai theo role.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tat CSRF vi day la REST API, khong dung form HTML
                // CSRF chi can thiet cho ung dung web truyen thong dung form
                .csrf(AbstractHttpConfigurer::disable)

                // Cau hinh quyen truy cap cho tung nhom API
                .authorizeHttpRequests(auth -> auth
                        // API auth (dang ky, dang nhap): ai cung goi duoc
                        .requestMatchers("/api/auth/**").permitAll()
                        // API test: ai cung goi duoc
                        .requestMatchers("/api/test/**").permitAll()
                        // Tat ca API khac: TAM THOI mo het de tien dev
                        // Sau nay se doi thanh .authenticated() khi lam JWT
                        .anyRequest().permitAll()
                )

                // Dung stateless session (khong luu session tren server)
                // Vi REST API nen moi request tu client phai gui token/thong tin xac thuc
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    /**
     * Tao bean PasswordEncoder su dung BCrypt.
     *
     * BCrypt la giai thuat ma hoa mat khau pho bien nhat:
     * - Mot chieu: khong the giai ma nguoc tu hash ve mat khau goc
     * - Co salt tu dong: 2 mat khau giong nhau se co hash khac nhau
     * - Chong brute-force: giai thuat cham (co chu dich) de tranh do mat khau
     *
     * Su dung: passwordEncoder.encode("123456") -> "$2a$10$..."
     *          passwordEncoder.matches("123456", hash) -> true/false
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
