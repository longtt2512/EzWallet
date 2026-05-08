# EzWallet Frontend

Angular 17 (standalone components, signals) - tương thích Node v20.

## Cài đặt + chạy

```bash
npm install
npm start          # Mở http://localhost:4200
```

## Cấu trúc

```
src/app/
├── core/              # Service, guard, interceptor, model dùng chung
├── shared/            # Component / pipe / directive tái sử dụng
├── layouts/           # Layout chính + auth layout
└── features/
    ├── auth/                  # [Member 1]
    ├── topup-withdraw/        # [Member 2]
    ├── transfer/              # [Member 3 - Long]
    ├── bill-payment/          # [Member 4]
    ├── transaction-history/
    └── dashboard/
```

## Path alias

| Alias       | Trỏ tới              |
|-------------|----------------------|
| `@core/*`   | `src/app/core/*`     |
| `@shared/*` | `src/app/shared/*`   |
| `@features/*` | `src/app/features/*` |
| `@env/*`    | `src/environments/*` |
