package com.travelmate.entity.enums;

public enum PartnerWithdrawalStatus {
    PENDING("Chờ Admin xử lý", "badge--warning"),
    PAID("Đã ghi nhận xử lý", "badge--success"),
    REJECTED("Đã từ chối", "badge--danger");

    private final String displayName;
    private final String badgeClass;

    PartnerWithdrawalStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
