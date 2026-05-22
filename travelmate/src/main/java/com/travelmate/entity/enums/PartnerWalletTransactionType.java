package com.travelmate.entity.enums;

public enum PartnerWalletTransactionType {
    SETTLEMENT_CREDIT("Cộng quyết toán"),
    WITHDRAWAL_REQUEST("Yêu cầu rút tiền"),
    WITHDRAWAL_PAID("Admin đã chuyển khoản"),
    WITHDRAWAL_REJECTED("Hoàn tiền rút bị từ chối");

    private final String displayName;

    PartnerWalletTransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
