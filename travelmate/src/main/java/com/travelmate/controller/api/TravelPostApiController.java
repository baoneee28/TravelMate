package com.travelmate.controller.api;

import com.travelmate.entity.TravelPost;
import com.travelmate.security.CustomUserDetails;
import com.travelmate.service.TravelPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * REST API cho tính năng quản lý bài viết du lịch.
 * GET    /api/travel-posts/{id}        — chi tiết bài viết (public — dùng cho modal user)
 * POST   /api/travel-posts             — thêm bài viết mới (admin only)
 * PUT    /api/travel-posts/{id}        — sửa bài viết (admin only)
 * POST   /api/travel-posts/{id}/toggle — ẩn/hiện bài viết (admin only)
 * DELETE /api/travel-posts/{id}        — xóa bài viết (admin only)
 */
@RestController
@RequestMapping("/api/travel-posts")
@RequiredArgsConstructor
public class TravelPostApiController {

    private final TravelPostService travelPostService;

    private boolean isAdmin(CustomUserDetails p) {
        return p != null && p.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Map<String, Object> toMap(TravelPost post) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           post.getId());
        m.put("title",        post.getTitle());
        m.put("destination",  post.getDestination() != null ? post.getDestination() : "");
        m.put("destinationName", post.getDestination() != null ? post.getDestination() : "");
        m.put("destinationSlug", post.getDestinationSlug() != null ? post.getDestinationSlug() : "");
        m.put("summary",      post.getSummary() != null ? post.getSummary() : "");
        m.put("content",      post.getContent() != null ? post.getContent() : "");
        m.put("thumbnailUrl", post.getThumbnailUrl() != null ? post.getThumbnailUrl() : "");
        m.put("sourceName",   post.getSourceName() != null ? post.getSourceName() : "");
        m.put("sourceUrl",    post.getSourceUrl());
        m.put("category",     post.getCategory().name());
        m.put("displayName",  post.getCategory().getDisplayName());
        m.put("status",       post.getStatus().name());
        m.put("createdBy",    post.getCreatedBy() != null ? post.getCreatedBy() : "");
        m.put("createdAt",    post.getCreatedAt() != null ? post.getCreatedAt().toString() : "");
        return m;
    }

    /** GET /api/travel-posts/{id} — chi tiết bài viết (public cho modal user) */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id,
                                       @AuthenticationPrincipal CustomUserDetails principal) {
        return (isAdmin(principal) ? travelPostService.findById(id) : travelPostService.findVisibleById(id))
                .map(post -> ResponseEntity.ok(toMap(post)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/travel-posts — admin thêm bài viết mới */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (!isAdmin(principal)) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        String title     = body.get("title");
        String destination = readDestination(body);
        String sourceUrl = body.get("sourceUrl");
        if (title == null || title.isBlank() || destination == null || destination.isBlank()
                || sourceUrl == null || sourceUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "title, destination và sourceUrl là bắt buộc"));
        }
        if (!isHttpUrl(sourceUrl)) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL nguồn phải là liên kết http/https hợp lệ"));
        }

        TravelPost post = travelPostService.create(
                title.trim(),
                body.getOrDefault("summary", ""),
                body.getOrDefault("content", ""),
                destination.trim(),
                body.getOrDefault("thumbnailUrl", ""),
                body.getOrDefault("sourceName", "Vietnam.travel"),
                sourceUrl.trim(),
                parseCategory(body.getOrDefault("category", "GUIDE")),
                principal.getUsername()
        );

        return ResponseEntity.ok(toMap(post));
    }

    /** PUT /api/travel-posts/{id} — admin cập nhật bài viết */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (!isAdmin(principal)) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        String title     = body.get("title");
        String destination = readDestination(body);
        String sourceUrl = body.get("sourceUrl");
        if (title == null || title.isBlank() || destination == null || destination.isBlank()
                || sourceUrl == null || sourceUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "title, destination và sourceUrl là bắt buộc"));
        }
        if (!isHttpUrl(sourceUrl)) {
            return ResponseEntity.badRequest().body(Map.of("error", "URL nguồn phải là liên kết http/https hợp lệ"));
        }

        TravelPost post = travelPostService.update(
                id,
                title.trim(),
                body.getOrDefault("summary", ""),
                body.getOrDefault("content", ""),
                destination.trim(),
                body.getOrDefault("thumbnailUrl", ""),
                body.getOrDefault("sourceName", "Vietnam.travel"),
                sourceUrl.trim(),
                parseCategory(body.getOrDefault("category", "GUIDE"))
        );

        return ResponseEntity.ok(toMap(post));
    }

    /** POST /api/travel-posts/{id}/toggle — ẩn/hiện bài viết */
    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (!isAdmin(principal)) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        TravelPost post = travelPostService.toggleStatus(id);
        return ResponseEntity.ok(Map.of(
                "id",     post.getId(),
                "status", post.getStatus().name()
        ));
    }

    /** DELETE /api/travel-posts/{id} — xóa bài viết */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (!isAdmin(principal)) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));

        travelPostService.delete(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private TravelPost.Category parseCategory(String value) {
        try {
            return TravelPost.Category.valueOf(value);
        } catch (Exception e) {
            return TravelPost.Category.GUIDE;
        }
    }

    private String readDestination(Map<String, String> body) {
        String destination = body.get("destination");
        if (destination == null || destination.isBlank()) {
            destination = body.get("destinationName");
        }
        return destination;
    }

    private boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && !"localhost".equals(normalizedHost)
                    && !normalizedHost.startsWith("127.");
        } catch (Exception e) {
            return false;
        }
    }
}
