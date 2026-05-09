package com.ezwallet.module.account.entity;

/**
 * Trạng thái tài khoản người dùng.
 *
 * <p>Sơ đồ chuyển trạng thái (cho test state transition):
 * <pre>
 *   PENDING_VERIFICATION ──verifyOtp()──► ACTIVE
 *   ACTIVE ──5 lần sai mật khẩu──► LOCKED
 *   LOCKED ──hết thời gian khoá──► ACTIVE
 *   ACTIVE / LOCKED ──admin ban──► BANNED (không thể quay lại)
 * </pre>
 */
public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    BANNED
}
