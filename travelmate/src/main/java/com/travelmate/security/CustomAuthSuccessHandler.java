package com.travelmate.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CustomAuthSuccessHandler — Xử lý redirect SAU KHI đăng nhập thành công.
 *
 * Vấn đề cũ: defaultSuccessUrl("/", true) → ai đăng nhập cũng về trang chủ.
 * Giải pháp: Kiểm tra role và redirect đến đúng trang:
 *   - ADMIN   → /admin/dashboard
 *   - PARTNER → /partner/rooms
 *   - USER    → / (trang chủ)
 *
 * Đây là pattern chuẩn trong Spring Security để phân quyền redirect.
 */
@Component
public class CustomAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    /**
     * Method này được Spring Security gọi ngay sau khi xác thực thành công.
     *
     * @param authentication — chứa thông tin user vừa đăng nhập (email, roles...)
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            response.sendRedirect("/admin/dashboard");

        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PARTNER"))) {
            response.sendRedirect("/partner/dashboard");

        } else {
            // USER: nếu trước đó bị redirect sang login (vd: cố vào /accommodations/1),
            // quay lại đúng trang đó sau khi đăng nhập thành công
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            if (savedRequest != null) {
                String targetUrl = savedRequest.getRedirectUrl();
                requestCache.removeRequest(request, response);
                response.sendRedirect(targetUrl);
            } else {
                response.sendRedirect("/");
            }
        }
    }
}
