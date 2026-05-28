package com.travelmate.security;

import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Google OAuth2 chỉ dành cho USER.
 * Partner/Admin vẫn đăng nhập bằng tài khoản hệ thống để tránh leo quyền qua OAuth.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    public OAuth2LoginSuccessHandler(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User oAuth2User)) {
            response.sendRedirect("/auth/login?oauth2Error=true");
            return;
        }

        String email = normalizeEmail(oAuth2User.getAttribute("email"));
        if (email == null) {
            response.sendRedirect("/auth/login?oauth2Error=email");
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> createGoogleUser(email, oAuth2User));
        if (user.getRole() != User.Role.USER) {
            SecurityContextHolder.clearContext();
            response.sendRedirect("/auth/login?oauth2Error=role");
            return;
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            SecurityContextHolder.clearContext();
            response.sendRedirect("/auth/login?oauth2Error=inactive");
            return;
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken travelMateAuth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(travelMateAuth);

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String targetUrl = savedRequest.getRedirectUrl();
            requestCache.removeRequest(request, response);
            response.sendRedirect(targetUrl);
            return;
        }

        response.sendRedirect("/");
    }

    private User createGoogleUser(String email, OAuth2User oAuth2User) {
        String name = firstText(oAuth2User.getAttribute("name"), oAuth2User.getAttribute("given_name"), "Google User");

        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setShortName(name);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPhone("");
        user.setRole(User.Role.USER);
        user.setStatus("ACTIVE");
        user.setAvatarUrl(oAuth2User.getAttribute("picture"));

        return userRepository.save(user);
    }

    private String normalizeEmail(Object rawEmail) {
        if (!(rawEmail instanceof String email) || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return "Google User";
    }
}
