package com.travelmate.entity.enums;

public enum WalletTransactionDirection {
    IN("Tiền vào"),
    OUT("Tiền ra"),
    INFO("Thông tin");

    private final String displayName;

    WalletTransactionDirection(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
