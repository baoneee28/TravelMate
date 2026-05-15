package com.travelmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * SupportTicket — quản lý yêu cầu hỗ trợ từ 3 nguồn:
 *   PARTNER: Partner gửi từ /partner/support (có partner field)
 *   USER:    User đăng nhập gửi từ /contact     (có user field)
 *   GUEST:   Khách vãng lai gửi từ /contact     (chỉ có requesterName/Email/Phone)
 */
@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nguồn gửi ticket: PARTNER / USER / GUEST */
    @Column(name = "requester_role", length = 20, nullable = false)
    private String requesterRole = "PARTNER";

    /** Partner gửi ticket — nullable nếu requesterRole != PARTNER */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_id", nullable = true)
    private User partner;

    /** User đăng nhập gửi liên hệ — nullable nếu requesterRole != USER */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /** Họ tên người gửi (dùng cho GUEST, hoặc override từ User/Partner name) */
    @Column(name = "requester_name", length = 255)
    private String requesterName;

    /** Email người gửi (dùng cho GUEST) */
    @Column(name = "requester_email", length = 255)
    private String requesterEmail;

    /** Số điện thoại người gửi (dùng cho GUEST/USER) */
    @Column(name = "requester_phone", length = 50)
    private String requesterPhone;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(nullable = false, length = 20)
    private String priority = "Trung bình";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Convenience helpers ───────────────────────────────────────────────────

    /** Trả về tên hiển thị của người gửi (ưu tiên: partner name > user name > requesterName) */
    public String getDisplayName() {
        if (partner != null && partner.getName() != null) return partner.getName();
        if (user != null && user.getName() != null) return user.getName();
        return requesterName != null ? requesterName : "Ẩn danh";
    }

    /** Trả về email hiển thị */
    public String getDisplayEmail() {
        if (partner != null && partner.getEmail() != null) return partner.getEmail();
        if (user != null && user.getEmail() != null) return user.getEmail();
        return requesterEmail != null ? requesterEmail : "";
    }
}
