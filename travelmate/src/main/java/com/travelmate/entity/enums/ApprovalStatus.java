package com.travelmate.entity.enums;

/**
 * ApprovalStatus — Trạng thái duyệt của nơi lưu trú.
 *
 * Khi partner đăng ký khách sạn mới → PENDING.
 * Admin duyệt → APPROVED → hiển thị cho user.
 * Admin từ chối → REJECTED.
 */
public enum ApprovalStatus {
    PENDING,    // Đang chờ quản trị viên duyệt
    APPROVED,   // Đã được duyệt, hiển thị cho user
    REJECTED    // Bị từ chối
}
