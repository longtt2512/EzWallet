# EzWallet - System Architecture

## High-Level Diagram

```
┌──────────────────────┐      HTTPS / JSON      ┌──────────────────────┐
│  Angular SPA (4200)  │ ────────────────────►  │  Spring Boot (8080)  │
│  - Standalone comps  │ ◄────────────────────  │  - JWT (Stateless)   │
│  - Signals           │                         │  - JPA / Flyway      │
│  - Material + Tailwind│                        │  - OpenAPI           │
└──────────────────────┘                         └──────┬───────┬───────┘
                                                        │       │
                       ┌────────────────────────────────┘       └─────────┐
                       ▼                                                  ▼
              ┌───────────────────┐                              ┌────────────────┐
              │  PostgreSQL 16    │                              │   MinIO (S3)   │
              │  (business DB)    │                              │   - Public     │
              └───────────────────┘                              │   - Private    │
                       │                                         └────────────────┘
                       ▼
              ┌───────────────────┐
              │     Redis 7       │
              │  - OTP cache      │
              │  - Rate limiting  │
              │  - Refresh tokens │
              └───────────────────┘

              ┌───────────────────┐
              │   MailHog (dev)   │
              │   Mock SMTP       │
              └───────────────────┘
```

## Backend Layers

```
controller  →  service  →  repository (JPA)  →  PostgreSQL
                  │
                  ├── OtpService (Redis)
                  ├── MailService (SMTP)
                  ├── FileService (MinIO)
                  └── FeeCalculator (in-memory)
```

## Module-Based Packaging

Unlike the traditional layer-based approach (top-level `controller/`, `service/`, `repository/` packages), code is grouped by *business module* so each functional area has clear boundaries:

```
com.ezwallet
├── EzWalletApplication
├── common      (ApiResponse, BaseEntity, Constants — shared utilities)
├── exception   (GlobalExceptionHandler)
├── config      (SecurityConfig, JwtConfig, MinioConfig, …)
└── module
    ├── auth          (Member 1) — register, login, OTP
    ├── account       (Shared)   — User, Wallet entities
    ├── topup         (Member 2)
    ├── transfer      (Member 3 — Long)
    ├── bill          (Member 4)
    └── transaction   (Shared)   — transaction history
```

## Security

- **Stateless JWT**: access token (15 minutes) + refresh token (7 days).
- **BCrypt** strength 12 for password hashing.
- **OTP** stored in Redis with a 5-minute TTL; SHA-256 hashed before comparison.
- **Idempotency Key** on POST transaction endpoints to prevent double-submission.
- **Rate limiting** via Redis: 5 failed login attempts per 15 minutes.
- **Optimistic locking** (`@Version`) on the Wallet entity to prevent race conditions when two transactions debit the same wallet concurrently.

## Business Data vs. Configuration

Business values (fees, limits) are **not** hard-coded in Java. They are stored in the `fee_rules` and `transaction_limits` tables so that decision-table and boundary-value test cases can be changed without modifying code.

## Transaction Boundaries

Every business flow that modifies a wallet balance (transfer, deposit, withdrawal, bill payment) is wrapped in `@Transactional` with `READ_COMMITTED` isolation. Optimistic locking (`@Version`) on the Wallet entity ensures that if two concurrent requests attempt to debit the same wallet, one of them will throw an `OptimisticLockException` and roll back.
