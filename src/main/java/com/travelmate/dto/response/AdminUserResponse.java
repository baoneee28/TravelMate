package com.travelmate.dto.response;

import com.travelmate.enums.Role;
import com.travelmate.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String roleLabel;
    private String roleBadgeClass;
    private UserStatus status;
    private String statusLabel;
    private boolean active;
    private long bookingCount;
    private LocalDateTime createdAt;
    private String avatarInitials;
    private String avatarColor;
}
