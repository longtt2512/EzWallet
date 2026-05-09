# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EzWallet is a simulated e-wallet application with an Angular frontend and Spring Boot backend. The codebase is structured to support common quality practices such as boundary value analysis, decision tables, state transition testing, and equivalence partitioning.

## Commands

### Infrastructure (Docker Compose)
```bash
make up          # Start postgres, minio, redis, pgadmin, mailhog
make down        # Stop all containers
make clean       # Remove all volumes (destroys DB data!)
```

### Backend (Spring Boot)
```bash
make be-run      # Run dev server at http://localhost:8080
make be-test     # Run all unit tests
make be-build    # Build jar (skip tests)

# Run a single test class
cd backend && ./gradlew test --tests "com.ptit.ezwallet.ClassName"
# Run a single test method
cd backend && ./gradlew test --tests "com.ptit.ezwallet.ClassName.methodName"
```

### Frontend (Angular)
```bash
make fe-install  # Install npm dependencies
make fe-run      # Dev server at http://localhost:4200
make fe-test     # Karma/Jasmine unit tests
make fe-build    # Production build

# CI test (headless, with coverage)
cd frontend && npm run test:ci
# E2E
cd frontend && npm run e2e
```

## Architecture

```
Angular 17 (standalone) ──REST/JSON──► Spring Boot 3.2 (Java 17)
                                              │
                                   ┌──────────┼──────────┐
                                   ▼          ▼          ▼
                               PostgreSQL   Redis      MinIO
                               (JPA+Flyway) (OTP/cache) (files/QR)
                                            MailHog (SMTP mock)
```

### Backend (`backend/`)

Package root: `com.ezwallet`

| Package | Purpose |
|---------|---------|
| `config/` | Spring Security (JWT stateless), Redis, MinIO, CORS, OpenAPI |
| `common/` | `ApiResponse<T>` (unified REST envelope), `BaseEntity` (JPA audit), `PageResponse<T>`, `Constants` |
| `exception/` | `BusinessException`, `ResourceNotFoundException`, `GlobalExceptionHandler` |
| `module/account/` | `User`, `Wallet` entities + `UserRepository`; enums `UserStatus`, `UserTier`, `WalletStatus` |
| `module/auth/` | `HealthController`, `AppUserDetailsService` |

**Module layout convention** (to be followed for all feature modules):
```
module/<feature>/
  controller/   ← REST controllers
  service/      ← business logic
  repository/   ← Spring Data JPA
  entity/       ← JPA entities
  dto/          ← request/response DTOs
  mapper/       ← MapStruct mappers
```

**Key design rules:**
- All REST responses use `ApiResponse<T>` — never return raw objects.
- All entities extend `BaseEntity` (auto-auditing: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `version` for optimistic locking).
- Throw `BusinessException(code, message, httpStatus)` for domain rule violations; `GlobalExceptionHandler` converts everything to `ApiResponse`.
- Security is stateless JWT. Public endpoints: `/auth/**`, `/actuator/health`, Swagger paths. Everything else requires `Authorization: Bearer <token>`.
- DB schema is managed exclusively by **Flyway** (`src/main/resources/db/migration/V*.sql`). JPA is set to `ddl-auto: validate` in dev.
- Fee rules and transaction limits are stored in the `fee_rules` and `transaction_limits` tables to support decision table testing without code changes.

**Test profiles:**
- Unit tests use H2 in-memory (`application-test.yml`), Flyway disabled, `ddl-auto: create-drop`.
- Integration tests use Testcontainers with `@DynamicPropertySource` to override datasource.

### Frontend (`frontend/`)

Angular 17 standalone components with lazy-loaded feature routes.

| Path | Feature |
|------|---------|
| `app/core/` | `AuthService` (signals), `authGuard`, interceptors (`auth`, `error`), shared models |
| `app/layouts/` | `MainLayoutComponent` (shell for authenticated pages) |
| `app/features/auth/` | Login, Register pages + `AuthLayoutComponent` |
| `app/features/dashboard/` | Wallet overview |
| `app/features/topup-withdraw/` | Top-up and withdrawal flows |
| `app/features/transfer/` | P2P transfer, QR payment |
| `app/features/bill-payment/` | Bill payment |
| `app/features/transaction-history/` | History + filter |

**Key design rules:**
- `AuthService` uses Angular `signal()` for `isAuthenticated` and `currentUser` — read these reactively in templates/guards.
- Tokens are stored in `localStorage` under keys `ezwallet.access` and `ezwallet.refresh` (defined in `environment.ts`).
- `authInterceptor` attaches `Authorization: Bearer` to all non-public requests automatically.
- `errorInterceptor` handles HTTP errors centrally.
- API base URL is `http://localhost:8080/api/v1` (from `environment.ts`).
- Uses Angular Material + Tailwind CSS; `@ngx-translate` for i18n.

## Domain Model

Core tables (Flyway `V1__init_schema.sql`):
- `users` — account with `status` (PENDING_VERIFICATION → ACTIVE → LOCKED → BANNED) and `tier` (BRONZE/SILVER/GOLD)
- `wallets` — 1-to-1 with user, `balance >= 0` enforced at DB level
- `bank_accounts` — linked bank cards per user
- `transactions` — single table for all types: TOPUP / WITHDRAW / TRANSFER / BILL_PAYMENT; uses `idempotency_key` for dedup
- `bill_providers` / `bills` — bill payment support
- `fee_rules` — decision-table-driven fee calculation (tx_type × tier × amount range)
- `transaction_limits` — per-transaction, daily, monthly limits by tier
- `otp_tokens` — OTP for REGISTER / RESET_PASSWORD / TRANSFER / WITHDRAW purposes

## Service URLs (local dev)

| Service | URL | Credentials |
|---------|-----|-------------|
| Frontend | http://localhost:4200 | — |
| Backend API | http://localhost:8080/api/v1 | — |
| Swagger UI | http://localhost:8080/swagger-ui.html | — |
| pgAdmin | http://localhost:5050 | admin@ezwallet.local / admin123 |
| MinIO UI | http://localhost:9001 | minioadmin / minioadmin |
| MailHog | http://localhost:8025 | — |
