package com.travelmate.enums;

/**
 * Enum dai dien cho trang thai duyet listing (noi luu tru).
 *
 * Khi partner tao listing moi, listing se co trang thai PENDING.
 * Admin se xem xet va chuyen sang APPROVED hoac REJECTED.
 *
 * - PENDING: dang cho duyet (mac dinh khi partner tao moi)
 * - APPROVED: da duoc admin duyet, listing se hien thi cho user
 * - REJECTED: bi admin tu choi, partner co the sua va gui lai
 *
 * Chi nhung listing co trang thai APPROVED moi duoc hien thi cong khai.
 */
public enum ApprovalStatus {

    PENDING,    // Dang cho duyet
    APPROVED,   // Da duoc duyet - hien thi cong khai
    REJECTED    // Bi tu choi - can sua lai
}
