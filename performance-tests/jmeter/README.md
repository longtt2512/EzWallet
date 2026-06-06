# JMeter Performance Tests - EzWallet

## Overview
Performance test suite for EzWallet modules using Apache JMeter 5.6.3+

## Test Plans

### 1. Transfer Module Performance Test
**File:** `testplans/Transfer-Performance-Test.jmx`

**Scenarios:**
- Request Transfer OTP
- P2P Transfer with OTP validation

**Configuration:**
- Threads: 50
- Ramp-up: 30 seconds
- Duration: 300 seconds (5 minutes)

**Test Data:** 
- `testdata/users.csv` - User credentials
- `testdata/transfer-data.csv` - Transfer details

### 2. QR Payment Performance Test
**File:** `testplans/QR-Performance-Test.jmx`

**Scenarios:**
- Generate QR Code
- Get QR Code Info
- Request OTP
- Pay with QR Code

**Configuration:**
- Threads: 30
- Ramp-up: 20 seconds
- Duration: 300 seconds (5 minutes)

**Test Data:**
- `testdata/users.csv` - User credentials
- Random amount generation (50,000 - 500,000 VND)

### 3. Transaction History Performance Test
**File:** `testplans/Transaction-History-Performance-Test.jmx`

**Scenarios:**
- Get all transactions (no filter)
- Filter by transaction type (TRANSFER)
- Filter by status (SUCCESS)
- Pagination testing

**Configuration:**
- Threads: 100
- Ramp-up: 30 seconds
- Duration: 300 seconds (5 minutes)

**Test Data:**
- `testdata/users.csv` - User credentials

## Prerequisites

1. **Install JMeter:**
```bash
# macOS
brew install jmeter

# Or download from https://jmeter.apache.org/download_jmeter.cgi
```

2. **Start Backend Services:**
```bash
# From project root
make up      # Start infrastructure (PostgreSQL, Redis, MinIO, MailHog)

# Start backend in PERFORMANCE MODE (fixed OTP = 123456)
make dev-perf

# OR start normally (random OTP)
make dev
```

3. **Prepare Test Data:**
Before running performance tests, you need active user accounts. You can either:
- Register users manually through the frontend
- Update `testdata/users.csv` with existing valid credentials

## Performance Mode

Backend supports a special `PERF_MODE` for performance testing:

**When PERF_MODE=true:**
- All OTP codes generated will be `123456` (fixed)
- No need to check MailHog or fetch real OTP
- Allows JMeter to run realistic load tests with OTP validation

**To enable:**
```bash
# Method 1: Use make command (Recommended)
make dev-perf

# Method 2: Use backend-only command
make be-run-perf

# Method 3: Set environment variable manually
export PERF_MODE=true
cd backend && ./gradlew bootRun
```

**⚠️ IMPORTANT:** Never enable `PERF_MODE` in production! This is only for load testing.

## Running Tests

### Using JMeter GUI (for test development/debugging)

```bash
cd performance-tests/jmeter/testplans

# Run Transfer test
jmeter -t Transfer-Performance-Test.jmx

# Run QR test
jmeter -t QR-Performance-Test.jmx

# Run Transaction History test
jmeter -t Transaction-History-Performance-Test.jmx
```

### Using JMeter CLI (for actual performance testing)

```bash
cd performance-tests/jmeter/testplans

# Transfer Module Test
jmeter -n -t Transfer-Performance-Test.jmx \
  -l ../results/transfer-results.jtl \
  -e -o ../results/transfer-html-report

# QR Module Test
jmeter -n -t QR-Performance-Test.jmx \
  -l ../results/qr-results.jtl \
  -e -o ../results/qr-html-report

# Transaction History Test
jmeter -n -t Transaction-History-Performance-Test.jmx \
  -l ../results/transaction-history-results.jtl \
  -e -o ../results/transaction-history-html-report
```

### Running with Custom Parameters

```bash
# Override thread count, ramp-up, and duration
jmeter -n -t Transfer-Performance-Test.jmx \
  -JTHREADS=100 \
  -JRAMP_UP=60 \
  -JDURATION=600 \
  -l ../results/transfer-results.jtl \
  -e -o ../results/transfer-html-report
```

## Test Results

Results are saved in `results/` directory:
- `*.jtl` - Raw test results (CSV format)
- `*-html-report/` - HTML dashboard reports
- `*-summary.csv` - Summary statistics

### Key Metrics to Monitor

1. **Throughput:** Requests per second
2. **Response Time:** Average, Median, 90th, 95th, 99th percentile
3. **Error Rate:** Percentage of failed requests
4. **Latency:** Time to first byte

## Test Data Files

### users.csv
```csv
email,password
user1@test.com,Password123!
user2@test.com,Password123!
...
```

### transfer-data.csv
```csv
recipientPhone,amount,note
0912345678,50000,Test transfer 1
0912345679,100000,Test transfer 2
...
```

## Important Notes

1. **OTP Handling:** 
   - **For Performance Testing:** Start backend with `PERF_MODE=true` using `./performance-tests/start-backend-perf.sh`
   - This makes all OTPs = `123456` (hardcoded in JMeter tests)
   - **Never enable PERF_MODE in production!**

2. **Data Cleanup:** Performance tests create real transactions. Clean up test data periodically:
```sql
-- Connect to PostgreSQL
DELETE FROM transactions WHERE created_at < NOW() - INTERVAL '1 day';
```

3. **Resource Monitoring:** Monitor system resources during tests:
```bash
# Backend logs
make be-run

# Database connections
docker exec -it ezwallet-postgres psql -U postgres -d ezwallet \
  -c "SELECT count(*) FROM pg_stat_activity;"

# Redis memory
docker exec -it ezwallet-redis redis-cli INFO memory
```

4. **Baseline Performance Targets:**
- Response time: < 500ms (p95)
- Throughput: > 100 req/sec
- Error rate: < 1%

## Troubleshooting

### Authentication Failures
- Verify users exist in database
- Check token expiration (tokens expire after 24h by default)
- Ensure backend is running and accessible

### High Error Rates
- Check backend logs for exceptions
- Verify database connection pool size
- Monitor Redis connections
- Check network latency

### OTP Errors
- Verify MailHog is running at http://localhost:8025
- Check OTP expiration (5 minutes default)
- Ensure OTP is valid for the operation type

## Performance Tuning Tips

1. **Backend:**
   - Increase database connection pool size
   - Enable query caching
   - Add database indexes
   - Tune JVM heap size

2. **JMeter:**
   - Run in non-GUI mode for accurate results
   - Increase Java heap: `export HEAP="-Xms512m -Xmx2048m"`
   - Use CSV output instead of XML
   - Disable unnecessary listeners

3. **Test Design:**
   - Use realistic think times
   - Distribute load gradually (proper ramp-up)
   - Test one module at a time
   - Run tests from separate machine (not localhost)
