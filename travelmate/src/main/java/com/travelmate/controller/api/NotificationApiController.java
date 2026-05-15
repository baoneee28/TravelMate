package com.travelmate.controller.api;

import com.travelmate.entity.Notification;
import com.travelmate.entity.User;
import com.travelmate.repository.UserRepository;
import com.travelmate.security.CustomUserDetails;
import com.travelmate.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Optional<User> resolveUser(CustomUserDetails principal) {
        if (principal == null) return Optional.empty();
        return userRepository.findByEmail(principal.getUsername());
    }

    /** GET /api/notifications — danh sách thông báo (tối đa 20 mục gần nhất) */
    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomUserDetails principal) {
        return resolveUser(principal).map(user -> {
            List<Notification> all = notificationService.getNotificationsForUser(user);
            List<Map<String, Object>> result = all.stream()
                    .limit(20)
                    .map(n -> Map.<String, Object>of(
                            "id",        n.getId(),
                            "title",     n.getTitle(),
                            "message",   n.getMessage() != null ? n.getMessage() : "",
                            "type",      n.getType().name(),
                            "targetUrl", n.getTargetUrl() != null ? n.getTargetUrl() : "/my-bookings",
                            "isRead",    n.getIsRead(),
                            "createdAt", n.getCreatedAt() != null ? n.getCreatedAt().format(FMT) : ""
                    ))
                    .toList();
            return ResponseEntity.ok(result);
        }).orElseGet(() -> ResponseEntity.status(401).build());
    }

    /** GET /api/notifications/count — số thông báo chưa đọc */
    @GetMapping("/count")
    public ResponseEntity<?> count(@AuthenticationPrincipal CustomUserDetails principal) {
        return resolveUser(principal)
                .map(user -> ResponseEntity.ok(Map.<String, Object>of(
                        "count", notificationService.getUnreadCount(user))))
                .orElseGet(() -> ResponseEntity.ok(Map.of("count", 0)));
    }

    /** POST /api/notifications/{id}/read — đánh dấu 1 thông báo đã đọc */
    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id,
                                      @AuthenticationPrincipal CustomUserDetails principal) {
        return resolveUser(principal).map(user -> {
            notificationService.markAsRead(id, user);
            return ResponseEntity.ok(Map.<String, Object>of("ok", true));
        }).orElseGet(() -> ResponseEntity.status(401).build());
    }

    /** POST /api/notifications/read-all — đánh dấu tất cả đã đọc */
    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal CustomUserDetails principal) {
        return resolveUser(principal).map(user -> {
            notificationService.markAllAsRead(user);
            return ResponseEntity.ok(Map.<String, Object>of("ok", true));
        }).orElseGet(() -> ResponseEntity.status(401).build());
    }
}
