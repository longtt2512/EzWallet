# EzWallet - Hợp đồng API (sẽ được cập nhật khi từng module hoàn thiện)

> Truy cập Swagger UI sau khi khởi động backend: http://localhost:8080/api/v1/swagger-ui.html

## Quy ước chung

- **Base URL**: `/api/v1`
- **Authentication**: `Authorization: Bearer <accessToken>` cho mọi endpoint trừ `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/otp/verify`.
- **Response format**: `ApiResponse<T>`
  ```json
  {
    "success": true,
    "code": "OK",
    "message": "Thành công",
    "data": { },
    "errors": null,
    "timestamp": "2026-05-08T08:00:00Z"
  }
  ```
- **Error codes**: `VALIDATION_FAILED`, `INVALID_CREDENTIALS`, `UNAUTHORIZED`, `ACCESS_DENIED`, `NOT_FOUND`, `DATA_CONFLICT`, `INTERNAL_ERROR`, hoặc mã nghiệp vụ tự định nghĩa.

## Endpoint dự kiến

### Auth (Member 1)
| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/auth/register` | Đăng ký tài khoản |
| POST | `/auth/login` | Đăng nhập |
| POST | `/auth/refresh` | Lấy access token mới |
| POST | `/auth/logout` | Đăng xuất (revoke refresh) |
| POST | `/auth/otp/send` | Gửi OTP |
| POST | `/auth/otp/verify` | Xác minh OTP |
| POST | `/auth/password/change` | Đổi mật khẩu |

### Account / Wallet (shared)
| Method | Path | Mô tả |
|--------|------|-------|
| GET    | `/account/me` | Lấy hồ sơ + ví |
| GET    | `/account/wallet` | Số dư ví |

### Top-up / Withdraw (Member 2)
| Method | Path | Mô tả |
|--------|------|-------|
| GET    | `/bank-accounts` | Danh sách thẻ liên kết |
| POST   | `/bank-accounts` | Liên kết thẻ |
| POST   | `/topup` | Nạp tiền |
| POST   | `/withdraw` | Rút tiền |

### Transfer (Member 3 - Long)
| Method | Path | Mô tả |
|--------|------|-------|
| POST   | `/transfer/internal` | Chuyển tiền trong ví |
| POST   | `/transfer/external` | Chuyển liên ngân hàng |
| POST   | `/transfer/qr` | Quét QR thanh toán merchant |

### Bill payment (Member 4)
| Method | Path | Mô tả |
|--------|------|-------|
| GET    | `/bills/lookup` | Tra cứu hoá đơn |
| POST   | `/bills/{id}/pay` | Thanh toán hoá đơn |
| GET    | `/bill-providers` | Danh sách NCC |

### Transaction (shared)
| Method | Path | Mô tả |
|--------|------|-------|
| GET    | `/transactions` | Lịch sử giao dịch (filter, paginate) |
| GET    | `/transactions/{ref}` | Chi tiết giao dịch |
