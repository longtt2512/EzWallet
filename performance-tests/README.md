# Performance Testing

This directory contains performance testing tools and documentation for EzWallet.

## Contents

- **jmeter/** - Apache JMeter test plans for load testing
  - Transfer Module (P2P transfers with OTP)
  - QR Payment Module (QR generation and payment)
  - Transaction History Module (filtering and pagination)
- **PERF_MODE.md** - Documentation about fixed OTP mode for testing
- **SETUP_GUIDE.md** - Complete step-by-step setup and testing guide
- **create-test-accounts.sh** - Script to create test accounts
- **add-balance-to-test-accounts.sql** - SQL to add balance to test accounts

## Quick Start

```bash
# 1. Start infrastructure and backend in performance mode
make dev-perf

# 2. Create test accounts (if not done yet)
cd performance-tests
./create-test-accounts.sh

# 3. Activate accounts and add balance
docker exec ezwallet-postgres psql -U ezwallet -d ezwallet -c \
  "UPDATE users SET status = 'ACTIVE' WHERE username IN ('user1', 'user2', 'user3', 'user4', 'user5');"
docker exec -i ezwallet-postgres psql -U ezwallet -d ezwallet < add-balance-to-test-accounts.sql

# 4. Verify accounts are ready
docker exec ezwallet-postgres psql -U ezwallet -d ezwallet -c \
  "SELECT u.username, u.email, u.status, w.balance FROM users u LEFT JOIN wallets w ON u.id = w.user_id WHERE u.username LIKE 'user%' ORDER BY u.username;"

# 5. Run JMeter tests
cd jmeter
./run-all-tests.sh
```

## Test Accounts

5 test accounts are pre-configured:

| Username | Password | Email | Balance |
|----------|----------|-------|---------|
| user1 | Password123! | user1@test.com | 50,000,000 VND |
| user2 | Password123! | user2@test.com | 50,000,000 VND |
| user3 | Password123! | user3@test.com | 50,000,000 VND |
| user4 | Password123! | user4@test.com | 50,000,000 VND |
| user5 | Password123! | user5@test.com | 50,000,000 VND |

**Note:** Login uses `username` (not email) as the identifier.

## What is PERF_MODE?

Performance mode enables fixed OTP generation (`123456`) to allow realistic load testing of OTP-protected endpoints without the overhead of email retrieval or OTP randomness.

**How to enable:**
```bash
# Full stack with PERF_MODE
make dev-perf

# Or backend only
make be-run-perf

# Or manual
export PERF_MODE=true
cd backend && ./gradlew bootRun
```

**Security:** PERF_MODE defaults to `false` and only activates via environment variable. Never enable in production.

See [PERF_MODE.md](PERF_MODE.md) for detailed implementation.

## JMeter Test Plans

### 1. Transfer Module (50 threads, 5 min)
- Login with JWT authentication
- Request Transfer OTP
- P2P Transfer with OTP validation

### 2. QR Payment (30 threads, 5 min)
- Generate QR Code
- Get QR Info
- Request OTP
- Pay with QR Code

### 3. Transaction History (100 threads, 5 min)
- Get all transactions (no filter)
- Filter by type (TRANSFER)
- Filter by status (SUCCESS)
- Pagination testing

## Running Tests

### GUI Mode (for debugging)
```bash
cd performance-tests/jmeter/testplans
jmeter

# In GUI:
# 1. File → Open → select test plan
# 2. Reduce THREADS to 5 for testing
# 3. Run → Start
# 4. View results in "View Results Tree" and "Summary Report"
```

### CLI Mode (for actual performance testing)
```bash
cd performance-tests/jmeter

# Run all tests
./run-all-tests.sh

# Or run individual tests
cd testplans
jmeter -n -t Transfer-Performance-Test.jmx \
  -l ../results/transfer-results.jtl \
  -e -o ../results/transfer-html-report
```

## Test Results

JMeter generates HTML reports in `jmeter/results/` with:
- Response time graphs
- Throughput metrics
- Error rates
- Percentile statistics (p50, p90, p95, p99)

### Viewing Results
```bash
cd performance-tests/jmeter/results
open transfer-html-report/index.html
open qr-html-report/index.html
open transaction-history-html-report/index.html
```

## Performance Targets

- **Response Time (p95):** < 500ms
- **Response Time (Average):** < 200ms
- **Throughput:** > 100 requests/second
- **Error Rate:** < 1%

## Troubleshooting

### Connection Refused
Backend not running → `make dev-perf`

### 401 Unauthorized
Check accounts are ACTIVE and have correct credentials

### OTP_INVALID
Backend not in PERF_MODE → restart with `make dev-perf`

### INSUFFICIENT_BALANCE
Add balance: `docker exec -i ezwallet-postgres psql -U ezwallet -d ezwallet < add-balance-to-test-accounts.sql`

## Documentation

- **SETUP_GUIDE.md** - Complete step-by-step guide with screenshots and troubleshooting
- **PERF_MODE.md** - Technical details about PERF_MODE implementation
- **jmeter/README.md** - JMeter-specific instructions and best practices
- **jmeter/VERIFICATION_CHECKLIST.md** - Endpoint verification checklist
