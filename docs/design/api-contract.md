# EzWallet - API Contract (updated as each module is completed)

> Access Swagger UI after starting the backend: http://localhost:8080/api/v1/swagger-ui.html

## General Conventions

- **Base URL**: `/api/v1`
- **Authentication**: `Authorization: Bearer <accessToken>` required on all endpoints except `/auth/login`, `/auth/register`, `/auth/refresh`, and `/auth/otp/verify`.
- **Response format**: `ApiResponse<T>`
  ```json
  {
    "success": true,
    "code": "OK",
    "message": "Success",
    "data": { },
    "errors": null,
    "timestamp": "2026-05-08T08:00:00Z"
  }
  ```
- **Error codes**: `VALIDATION_FAILED`, `INVALID_CREDENTIALS`, `UNAUTHORIZED`, `ACCESS_DENIED`, `NOT_FOUND`, `DATA_CONFLICT`, `INTERNAL_ERROR`, or custom business-domain codes.

## Expected Endpoints

### Auth (Member 1)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register`          | Create a new account |
| POST | `/auth/login`             | Log in |
| POST | `/auth/refresh`           | Obtain a new access token |
| POST | `/auth/logout`            | Log out (revoke refresh token) |
| POST | `/auth/otp/send`          | Send OTP |
| POST | `/auth/otp/verify`        | Verify OTP |
| POST | `/auth/password/change`   | Change password |

### Account / Wallet (shared)
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/account/me`     | Get profile + wallet info |
| GET    | `/account/wallet` | Get wallet balance |

### Top-up / Withdraw (Member 2)
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/bank-accounts`      | List linked bank accounts |
| POST   | `/bank-accounts`      | Link a bank account |
| POST   | `/topup`              | Deposit funds |
| POST   | `/withdraw`           | Withdraw funds |

### Transfer (Member 3 — Long)
| Method | Path | Description |
|--------|------|-------------|
| POST   | `/transfer/internal` | Internal wallet-to-wallet transfer |
| POST   | `/transfer/external` | Inter-bank transfer |
| POST   | `/transfer/qr`       | QR code payment to a merchant |

### Bill Payment (Member 4)
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/bills/lookup`       | Look up a bill |
| POST   | `/bills/{id}/pay`     | Pay a bill |
| GET    | `/bill-providers`     | List bill providers |

### Transactions (shared)
| Method | Path | Description |
|--------|------|-------------|
| GET    | `/transactions`        | Transaction history (filterable, paginated) |
| GET    | `/transactions/{ref}`  | Transaction detail |
