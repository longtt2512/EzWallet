# EzWallet - Kiến trúc hệ thống

## Sơ đồ tổng thể

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
              │  (DB nghiệp vụ)   │                              │   - Public     │
              └───────────────────┘                              │   - Private    │
                       │                                         └────────────────┘
                       ▼
              ┌───────────────────┐
              │     Redis 7       │
              │  - Cache OTP      │
              │  - Rate limiting  │
              │  - Refresh tokens │
              └───────────────────┘

              ┌───────────────────┐
              │   MailHog (dev)   │
              │   SMTP giả lập    │
              └───────────────────┘
```

## Layer trong backend

```
controller  →  service  →  repository (JPA)  →  PostgreSQL
                  │
                  ├── OtpService (Redis)
                  ├── MailService (SMTP)
                  ├── FileService (MinIO)
                  └── FeeCalculator (in-memory)
```

## Module-based packaging

Khác với layer-based truyền thống (`controller/`, `service/`, `repository/` ở top-level), code được nhóm theo *module nghiệp vụ* để các phần chức năng có ranh giới rõ ràng:

```
com.ezwallet
├── EzWalletApplication
├── common      (ApiResponse, BaseEntity, Constants - dùng chung)
├── exception   (GlobalExceptionHandler)
├── config      (SecurityConfig, JwtConfig, MinioConfig, ...)
└── module
    ├── auth          (Member 1) - đăng ký, đăng nhập, OTP
    ├── account       (Shared)   - User, Wallet entity
    ├── topup         (Member 2)
    ├── transfer      (Member 3 - Long)
    ├── bill          (Member 4)
    └── transaction   (Shared)   - lịch sử
```

## Bảo mật

- **Stateless JWT**: access token (15 phút) + refresh token (7 ngày).
- **BCrypt** strength 12 cho password.
- **OTP** lưu trong Redis với TTL 5 phút, hash bằng SHA-256 trước khi compare.
- **Idempotency Key** trên POST giao dịch (chặn double-submit).
- **Rate limit** bằng Redis: 5 lần đăng nhập sai / 15 phút.
- **Optimistic locking** (`@Version`) trên Wallet để tránh race condition khi 2 giao dịch trừ tiền cùng lúc.

## Dữ liệu nghiệp vụ vs cấu hình

Các giá trị nghiệp vụ (phí, hạn mức) **không** hard-code trong Java mà nằm ở bảng `fee_rules`, `transaction_limits` — để test bảng quyết định / biên giá trị có thể thay đổi case mà không sửa code.

## Ranh giới giao dịch (Transaction)

Mỗi luồng nghiệp vụ làm thay đổi balance ví (chuyển tiền, nạp, rút, thanh toán hoá đơn) đều bọc trong `@Transactional` với mức `READ_COMMITTED`. Optimistic locking `@Version` trên Wallet đảm bảo nếu 2 request đồng thời trừ tiền cùng 1 ví, 1 trong 2 sẽ throw `OptimisticLockException` và rollback.
