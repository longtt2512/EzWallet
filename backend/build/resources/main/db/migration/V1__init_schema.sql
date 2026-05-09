-- ============================================================================
-- V1__init_schema.sql
-- Khởi tạo schema ban đầu cho EzWallet:
--   - users               : người dùng hệ thống
--   - wallets             : ví của người dùng (1-1)
--   - bank_accounts       : thẻ/tài khoản ngân hàng liên kết
--   - transactions        : giao dịch (cha cho mọi loại nạp/rút/chuyển/thanh toán)
--   - bill_providers      : nhà cung cấp dịch vụ (điện, nước, internet, ...)
--   - bills               : hoá đơn cụ thể
--   - fee_rules           : rule phí - đẩy ra DB để dễ test bảng quyết định
--   - transaction_limits  : hạn mức theo hạng tài khoản
--   - otp_tokens          : OTP đăng ký / chuyển khoản
-- ============================================================================

-- ----- Users -----
CREATE TABLE users (
    id                       BIGSERIAL PRIMARY KEY,
    username                 VARCHAR(50)  NOT NULL UNIQUE,
    email                    VARCHAR(150) NOT NULL UNIQUE,
    phone                    VARCHAR(20)  NOT NULL UNIQUE,
    password_hash            VARCHAR(100) NOT NULL,
    full_name                VARCHAR(150),
    status                   VARCHAR(30)  NOT NULL,
    tier                     VARCHAR(20)  NOT NULL DEFAULT 'BRONZE',
    failed_login_attempts    INT          NOT NULL DEFAULT 0,
    locked_until             TIMESTAMPTZ,
    last_login_at            TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ,
    created_by               VARCHAR(100),
    updated_by               VARCHAR(100),
    version                  BIGINT
);

CREATE INDEX idx_users_status ON users(status);

-- ----- Wallets -----
CREATE TABLE wallets (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance     NUMERIC(19,2) NOT NULL DEFAULT 0,
    currency    VARCHAR(10)  NOT NULL DEFAULT 'VND',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT,
    CONSTRAINT chk_wallet_balance_non_negative CHECK (balance >= 0)
);

-- ----- Bank accounts -----
CREATE TABLE bank_accounts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bank_code       VARCHAR(20)  NOT NULL,
    account_number  VARCHAR(30)  NOT NULL,
    account_holder  VARCHAR(150) NOT NULL,
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT,
    CONSTRAINT uk_bank_user_acct UNIQUE (user_id, bank_code, account_number)
);

CREATE INDEX idx_bank_user ON bank_accounts(user_id);

-- ----- Transactions -----
-- Bảng cha cho mọi loại giao dịch:
--   TOPUP / WITHDRAW / TRANSFER / BILL_PAYMENT
CREATE TABLE transactions (
    id              BIGSERIAL PRIMARY KEY,
    transaction_ref VARCHAR(50)  NOT NULL UNIQUE,
    type            VARCHAR(30)  NOT NULL,
    status          VARCHAR(30)  NOT NULL,
    amount          NUMERIC(19,2) NOT NULL,
    fee             NUMERIC(19,2) NOT NULL DEFAULT 0,
    currency        VARCHAR(10)  NOT NULL DEFAULT 'VND',
    source_wallet_id  BIGINT REFERENCES wallets(id),
    target_wallet_id  BIGINT REFERENCES wallets(id),
    bank_account_id   BIGINT REFERENCES bank_accounts(id),
    description       VARCHAR(500),
    failure_reason    VARCHAR(255),
    idempotency_key   VARCHAR(100) UNIQUE,
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT,
    CONSTRAINT chk_tx_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_tx_source ON transactions(source_wallet_id, created_at);
CREATE INDEX idx_tx_target ON transactions(target_wallet_id, created_at);
CREATE INDEX idx_tx_status ON transactions(status);
CREATE INDEX idx_tx_type ON transactions(type);

-- ----- Bill providers -----
CREATE TABLE bill_providers (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    service_type  VARCHAR(30)  NOT NULL,   -- ELECTRICITY / WATER / INTERNET / TELCO
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    version       BIGINT
);

-- ----- Bills -----
CREATE TABLE bills (
    id            BIGSERIAL PRIMARY KEY,
    bill_code     VARCHAR(50)  NOT NULL UNIQUE,
    provider_id   BIGINT NOT NULL REFERENCES bill_providers(id),
    customer_code VARCHAR(50)  NOT NULL,
    amount        NUMERIC(19,2) NOT NULL,
    status        VARCHAR(20)  NOT NULL,   -- UNPAID / PAID / EXPIRED / CANCELLED
    due_date      DATE,
    paid_at       TIMESTAMPTZ,
    paid_by_user  BIGINT REFERENCES users(id),
    paid_by_tx    BIGINT REFERENCES transactions(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    version       BIGINT
);

CREATE INDEX idx_bills_customer ON bills(provider_id, customer_code);
CREATE INDEX idx_bills_status ON bills(status);

-- ----- Fee rules: dữ liệu cho bảng quyết định -----
-- Rule phí dạng (tx_type × tier × min_amount × max_amount) → fee_type + fee_value
CREATE TABLE fee_rules (
    id            BIGSERIAL PRIMARY KEY,
    tx_type       VARCHAR(30)  NOT NULL,
    tier          VARCHAR(20)  NOT NULL,
    min_amount    NUMERIC(19,2) NOT NULL,
    max_amount    NUMERIC(19,2) NOT NULL,
    fee_type      VARCHAR(20)  NOT NULL,   -- FIXED / PERCENT
    fee_value     NUMERIC(19,4) NOT NULL,  -- Số tiền cố định hoặc % (0.005 = 0.5%)
    fee_min       NUMERIC(19,2) NOT NULL DEFAULT 0,
    fee_max       NUMERIC(19,2),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    effective_to   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    version        BIGINT,
    CONSTRAINT chk_fee_amount_range CHECK (min_amount <= max_amount)
);

CREATE INDEX idx_fee_rules_lookup ON fee_rules(tx_type, tier, active);

-- ----- Transaction limits theo hạng -----
CREATE TABLE transaction_limits (
    id                BIGSERIAL PRIMARY KEY,
    tier              VARCHAR(20) NOT NULL,
    tx_type           VARCHAR(30) NOT NULL,
    per_transaction   NUMERIC(19,2) NOT NULL,
    per_day           NUMERIC(19,2) NOT NULL,
    per_month         NUMERIC(19,2) NOT NULL,
    daily_count_limit INT,
    active            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT,
    CONSTRAINT uk_tx_limit_tier_type UNIQUE (tier, tx_type)
);

-- ----- OTP tokens -----
CREATE TABLE otp_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id) ON DELETE CASCADE,
    contact     VARCHAR(150) NOT NULL,    -- email hoặc phone
    purpose     VARCHAR(30)  NOT NULL,    -- REGISTER / RESET_PASSWORD / TRANSFER / WITHDRAW
    code_hash   VARCHAR(100) NOT NULL,
    attempts    INT          NOT NULL DEFAULT 0,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT
);

CREATE INDEX idx_otp_user_purpose ON otp_tokens(user_id, purpose, used);
