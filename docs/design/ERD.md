# EzWallet - Sơ đồ thực thể (ERD)

## Tổng quan

```
┌──────────────┐       1   1 ┌─────────────┐
│    users     │ ───────────►│   wallets   │
└──────┬───────┘             └──────┬──────┘
       │1                            │1
       │                             │N
       │N                            ▼
┌──────▼────────┐           ┌────────────────┐
│ bank_accounts │           │  transactions  │
└───────────────┘           └────────┬───────┘
                                     │
                              ┌──────┴──────┐
                              │             │
                              ▼             ▼
                       ┌────────────┐ ┌────────────────┐
                       │   bills    │ │ bill_providers │
                       └────────────┘ └────────────────┘

┌──────────────┐         ┌────────────────────┐         ┌──────────────┐
│ otp_tokens   │         │ transaction_limits │         │  fee_rules   │
│ (lookup tbl) │         │ (lookup tbl)       │         │ (lookup tbl) │
└──────────────┘         └────────────────────┘         └──────────────┘
```

## Bảng chính

### users
Thông tin người dùng. Trạng thái `status` hỗ trợ test state transition: `PENDING_VERIFICATION → ACTIVE → LOCKED → ACTIVE → BANNED`. Trường `failed_login_attempts` + `locked_until` dùng cho rule khoá tài khoản (5 lần sai → khoá 15 phút).

### wallets
Mỗi user có 1 ví duy nhất. CHECK constraint `balance >= 0` ở DB để đảm bảo không bao giờ âm.

### transactions
Bảng *cha* cho mọi loại giao dịch — phân loại bằng `type` (TOPUP / WITHDRAW / TRANSFER / BILL_PAYMENT). Trạng thái `status` chuyển: `PENDING → SUCCESS / FAILED / REFUNDED`. Có `idempotency_key` để chặn double-submit.

### fee_rules (bảng quyết định cho test)
Mỗi rule đại diện 1 hàng trong bảng quyết định khi tính phí: `(tx_type × tier × min_amount × max_amount) → fee_value`. Đẩy ra DB để dễ thêm test case mà không sửa code.

### transaction_limits (test biên giá trị)
Hạn mức `(tier × tx_type) → per_transaction / per_day / per_month`. Test biên dùng các giá trị: `per_transaction - 1`, `per_transaction`, `per_transaction + 1`.

### otp_tokens
Hash của OTP (không lưu plaintext), `attempts` để chặn brute-force, `expires_at` cho TTL 5 phút.

## Mapping với kỹ thuật kiểm thử

| Bảng/Trường | Kỹ thuật áp dụng |
|---|---|
| `users.status`        | State transition (4 trạng thái, nhiều phép chuyển) |
| `users.failed_login_attempts` | Boundary value (0, 4, 5, 6) |
| `transactions.status` | State transition (PENDING → ...) |
| `fee_rules`           | Decision table (mỗi rule = 1 dòng) |
| `transaction_limits`  | Boundary value, equivalence partitioning |
| `wallets.balance`     | Boundary (0, 1, max) + CHECK constraint test |
| `bills.status`        | State transition (UNPAID → PAID/EXPIRED/CANCELLED) |
