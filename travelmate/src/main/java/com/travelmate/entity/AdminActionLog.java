package com.travelmate.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AdminActionLog — Bảng ghi lại mọi thao tác của Admin trong hệ thống.
 *
 * Mục đích: Audit trail — giảng viên/admin có thể xem lại lịch sử thao tác.
 * VD: "Admin duyệt booking BK-LATA-DLX-0001", "Admin khóa user #5", v.v.
 */
@Entity
@Table(name = "admin_action_logs")
@Getter
@Setter
@NoArgsConstructor
public class AdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên admin thực hiện (email) */
    @Column(name = "admin_email", nullable = false, length = 100)
    private String adminEmail;

    /** Loại hành động: APPROVE_BOOKING, REJECT_BOOKING, LOCK_USER, ... */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    /** Loại đối tượng: BOOKING, ACCOMMODATION, ROOM, USER, SETTLEMENT, VOUCHER, SUPPORT_TICKET */
    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    /** ID đối tượng */
    @Column(name = "target_id")
    private Long targetId;

    /** Mô tả ngắn gọn hành động */
    @Column(name = "description", length = 500)
    private String description;

    /** Ghi chú thêm (VD: lý do từ chối) */
    @Column(name = "note", length = 1000)
    private String note;

    /** Thời gian thực hiện */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public AdminActionLog(String adminEmail, String actionType, String targetType,
                          Long targetId, String description, String note) {
        this.adminEmail = adminEmail;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.description = description;
        this.note = note;
    }
}
