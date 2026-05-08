package com.ptit.ezwallet.module.account.entity;

public enum WalletStatus {
    /** Hoạt động bình thường */
    ACTIVE,
    /** Tạm khoá, không cho giao dịch */
    FROZEN,
    /** Đóng vĩnh viễn */
    CLOSED
}
