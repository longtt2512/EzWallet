# EzWallet

Ứng dụng ví điện tử mô phỏng — Bài tập lớn môn **Đảm bảo chất lượng phần mềm (INT1416)** — Học viện Công nghệ Bưu chính Viễn thông.

## Mục tiêu

Xây dựng một hệ thống ví điện tử có đủ chức năng cốt lõi để áp dụng các kỹ thuật đảm bảo chất lượng phần mềm: rà soát tài liệu, rà soát code, kiểm thử hộp đen, kiểm thử hộp trắng, kiểm thử giao diện, kiểm thử tự động.

## Kiến trúc

```
┌────────────────────┐     HTTPS / REST     ┌──────────────────────┐
│  Angular Frontend  │ ───────────────────► │  Spring Boot Backend │
│  (Node 20, Ng 17)  │ ◄─────────────────── │   (Java 17, JPA)     │
└────────────────────┘                      └──────┬───────┬───────┘
                                                   │       │
                                          ┌────────┘       └─────────┐
                                          ▼                          ▼
                                 ┌─────────────────┐         ┌──────────────┐
                                 │  PostgreSQL 16  │         │   MinIO      │
                                 │  (dữ liệu chính)│         │ (file/QR/IMG)│
                                 └─────────────────┘         └──────────────┘
                                          │
                                          ▼
                                 ┌─────────────────┐
                                 │     Redis 7     │
                                 │ (OTP, rate-lim) │
                                 └─────────────────┘
```

## Phân chia chức năng (BTL nhóm 4 người)

| TV | Module | Chức năng | Kỹ thuật test trọng tâm |
|----|--------|-----------|------------------------|
| 1  | `auth`     | Đăng ký, đăng nhập, OTP, đổi mật khẩu, khoá tài khoản | BVA, decision table, state transition |
| 2  | `topup`    | Nạp tiền / Rút tiền, liên kết thẻ-ngân hàng           | EP, BVA, decision table (phí), pairwise |
| 3  | `transfer` | Chuyển tiền P2P, quét QR thanh toán                   | BVA, state transition, decision table   |
| 4  | `bill`     | Thanh toán hoá đơn, lịch sử giao dịch, lọc            | Decision table, state transition, BVA   |

## Yêu cầu môi trường

- Docker + Docker Compose
- Java 17 (khuyến nghị Temurin)
- Maven 3.9+ (hoặc dùng `./mvnw` đi kèm)
- Node.js 20.x + npm 10.x
- (Tuỳ chọn) Make

## Khởi chạy nhanh

```bash
# 1. Sao chép biến môi trường
cp .env.example .env

# 2. Khởi động hạ tầng (Postgres, MinIO, Redis, MailHog, pgAdmin)
make up
# Hoặc: docker compose up -d

# 3. Chạy backend
make be-run
# Backend: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html

# 4. Cài đặt và chạy frontend (terminal khác)
make fe-install
make fe-run
# Frontend: http://localhost:4200
```

## Các URL dịch vụ

| Dịch vụ      | URL                           | Tài khoản mặc định            |
|--------------|-------------------------------|--------------------------------|
| Frontend     | http://localhost:4200         | -                              |
| Backend API  | http://localhost:8080/api/v1  | -                              |
| Swagger UI   | http://localhost:8080/swagger-ui.html | -                       |
| pgAdmin      | http://localhost:5050         | admin@ezwallet.local / admin123|
| MinIO UI     | http://localhost:9001         | minioadmin / minioadmin        |
| MailHog UI   | http://localhost:8025         | -                              |

## Cấu trúc thư mục

```
EzWallet/
├── backend/         # Spring Boot 3.2 + Java 17
├── frontend/        # Angular 17 (standalone)
├── docs/
│   ├── design/      # ERD, kiến trúc, hợp đồng API
│   ├── sqa/         # Test plan, checklist rà soát, test case
│   └── postman/     # Bộ test API
├── docker-compose.yml
└── Makefile
```

## Tài liệu liên quan

- [Kiến trúc](docs/design/architecture.md)
- [ERD](docs/design/ERD.md)
- [Hợp đồng API](docs/design/api-contract.md)
- [Kế hoạch kiểm thử](docs/sqa/test-plan.md)

## License

Phần mềm chỉ dùng cho mục đích học tập (môn INT1416 - PTIT). Không dùng cho thương mại.
