package com.travelmate.enums;

/**
 * Enum dai dien cho trang thai tai khoan nguoi dung.
 *
 * - ACTIVE: tai khoan dang hoat dong binh thuong
 * - INACTIVE: tai khoan tam ngung (vi du: chua xac nhan email)
 * - BLOCKED: tai khoan bi khoa boi admin (vi pham chinh sach)
 *
 * Mac dinh khi dang ky, user se co trang thai ACTIVE.
 * Admin co quyen doi trang thai user thanh BLOCKED neu can.
 */
public enum UserStatus {

    ACTIVE,     // Tai khoan dang hoat dong
    INACTIVE,   // Tai khoan tam ngung
    BLOCKED     // Tai khoan bi khoa
}
