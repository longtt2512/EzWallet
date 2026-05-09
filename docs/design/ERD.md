# EzWallet - Entity Relationship Diagram (ERD)

## Overview

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

## Core Tables

### users
Stores user account information. The `status` field supports state transition testing: `PENDING_VERIFICATION → ACTIVE → LOCKED → ACTIVE → BANNED`. The `failed_login_attempts` and `locked_until` fields implement the account lock rule (5 failed attempts → locked for 15 minutes).

### wallets
Each user has exactly one wallet. A `CHECK` constraint (`balance >= 0`) is enforced at the database level to guarantee the balance never goes negative.

### transactions
The *parent* table for all transaction types — categorised by `type` (TOPUP / WITHDRAW / TRANSFER / BILL_PAYMENT). The `status` field transitions: `PENDING → SUCCESS / FAILED / REFUNDED`. An `idempotency_key` column prevents double-submission.

### fee_rules (decision-table lookup)
Each row represents one entry in the fee decision table: `(tx_type × tier × min_amount × max_amount) → fee_value`. Storing rules in the database makes it easy to add test cases without modifying code.

### transaction_limits (boundary value testing)
Defines limits `(tier × tx_type) → per_transaction / per_day / per_month`. Boundary value tests use: `per_transaction - 1`, `per_transaction`, `per_transaction + 1`.

### otp_tokens
Stores a hash of the OTP (never plaintext), an `attempts` counter to prevent brute-force, and `expires_at` for a 5-minute TTL.

## Mapping to Testing Techniques

| Table / Field | Technique Applied |
|---|---|
| `users.status`                | State transition (4 states, multiple transitions) |
| `users.failed_login_attempts` | Boundary value analysis (0, 4, 5, 6) |
| `transactions.status`         | State transition (PENDING → …) |
| `fee_rules`                   | Decision table (each row = one rule) |
| `transaction_limits`          | Boundary value analysis, equivalence partitioning |
| `wallets.balance`             | Boundary (0, 1, max) + CHECK constraint test |
| `bills.status`                | State transition (UNPAID → PAID / EXPIRED / CANCELLED) |
