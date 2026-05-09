# EzWallet

A simulated e-wallet application with a Spring Boot backend and Angular frontend.

## Objective

Build an e-wallet system with all core features: user registration, login, top-up/withdrawal, P2P transfer, bill payment, and transaction history.

## Architecture

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
                                 │  (primary data) │         │ (file/QR/IMG)│
                                 └─────────────────┘         └──────────────┘
                                          │
                                          ▼
                                 ┌─────────────────┐
                                 │     Redis 7     │
                                 │ (OTP, rate-lim) │
                                 └─────────────────┘
```

## Feature Modules

| # | Module | Features | Primary Test Techniques |
|---|--------|----------|------------------------|
| 1 | `auth`     | Registration, login, OTP, change password, account lock | BVA, decision table, state transition |
| 2 | `topup`    | Top-up / Withdrawal, bank account linking                | EP, BVA, decision table (fees), pairwise |
| 3 | `transfer` | P2P transfer, QR code payment                           | BVA, state transition, decision table   |
| 4 | `bill`     | Bill payment, transaction history, filtering             | Decision table, state transition, BVA   |

## Prerequisites

- Docker + Docker Compose
- Java 17 (Temurin recommended)
- Maven 3.9+ (or use the bundled `./mvnw`)
- Node.js 20.x + npm 10.x
- (Optional) Make

## Quick Start

```bash
make dev
```

That's it. The command will:
1. Create `.env` from `.env.example` if it doesn't exist (all defaults work out of the box)
2. Start Docker infrastructure (Postgres, MinIO, Redis, MailHog, pgAdmin)
3. Install frontend dependencies if `node_modules` is missing
4. Launch backend and frontend in parallel, with prefixed log output

Press **Ctrl+C** to stop both processes. To stop Docker containers as well: `make down`.

> **Individual commands** (if you need to run services separately):
> ```bash
> make up          # infrastructure only
> make be-run      # backend only  (http://localhost:8080)
> make fe-run      # frontend only (http://localhost:4200)
> make down        # stop all Docker containers
> ```

## After Making Changes

| What changed | Action |
|---|---|
| Frontend (`.ts`, `.html`, `.scss`) | **Nothing** — Angular dev server hot-reloads automatically |
| Backend (`.java`, `application.yml`) | Press **Ctrl+C** then `make dev` again, or restart just the backend with `make be-run` |
| Database schema (`V*.sql` Flyway migration) | Press **Ctrl+C**, then `make dev` — Flyway applies new migrations on startup |
| Docker / infra config (`docker-compose.yml`) | `make down && make dev` |
| Added npm package | `make fe-install`, then `make fe-run` (or `make dev`) |

## Service URLs

| Service      | URL                                   | Default Credentials            |
|--------------|---------------------------------------|--------------------------------|
| Frontend     | http://localhost:4200                 | -                              |
| Backend API  | http://localhost:8080/api/v1          | -                              |
| Swagger UI   | http://localhost:8080/swagger-ui.html | -                              |
| pgAdmin      | http://localhost:5050                 | admin@ezwallet.local / admin123|
| MinIO UI     | http://localhost:9001                 | minioadmin / minioadmin        |
| MailHog UI   | http://localhost:8025                 | -                              |

## Directory Structure

```
EzWallet/
├── backend/         # Spring Boot 3.2 + Java 17
├── frontend/        # Angular 17 (standalone)
├── docs/
│   ├── design/      # ERD, architecture, API contract
│   ├── qa/          # Test plan, review checklist, test cases
│   └── postman/     # API test collection
├── docker-compose.yml
└── Makefile
```

## Related Documentation

- [Architecture](docs/design/architecture.md)
- [ERD](docs/design/ERD.md)
- [API Contract](docs/design/api-contract.md)
- [Test Plan](docs/qa/test-plan.md)

## License

For educational and experimental use only. Not for commercial use.
