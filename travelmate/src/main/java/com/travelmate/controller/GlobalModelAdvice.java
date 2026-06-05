package com.travelmate.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {

    @ModelAttribute("isAdmin")
    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    @ModelAttribute("showAdminPreviewBar")
    public boolean showAdminPreviewBar(Authentication authentication, HttpServletRequest request) {
        return hasRole(authentication, "ROLE_ADMIN") && isPublicUserPage(request);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                    .anyMatch(authority -> role.equals(authority.getAuthority()));
    }

    private boolean isPublicUserPage(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String uri = request.getRequestURI();
        String path = uri != null && uri.startsWith(contextPath)
                ? uri.substring(contextPath.length())
                : uri;

        if (path == null || path.isBlank()) {
            return false;
        }
        return path.equals("/")
                || path.equals("/accommodations")
                || path.startsWith("/accommodations/")
                || path.equals("/travel")
                || path.equals("/news")
                || path.equals("/vouchers")
                || path.equals("/contact");
    }
}
