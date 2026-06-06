# Hướng dẫn chi tiết chạy Performance Test với JMeter

## 🎯 Mục lục
1. [Cài đặt JMeter](#bước-1-cài-đặt-jmeter)
2. [Chuẩn bị Test Accounts](#bước-2-chuẩn-bị-test-accounts)
3. [Start Backend với PERF_MODE](#bước-3-start-backend-với-perf_mode)
4. [Chạy Test với JMeter GUI](#bước-4-chạy-test-với-jmeter-gui)
5. [Chạy Test với JMeter CLI](#bước-5-chạy-test-với-jmeter-cli-production-mode)
6. [Xem và Phân tích Kết quả](#bước-6-xem-và-phân-tích-kết-quả)

---

## Bước 1: Cài đặt JMeter

### macOS:
```bash
# Cài qua Homebrew
brew install jmeter

# Verify
jmeter --version
```

### Windows:
1. Download từ: https://jmeter.apache.org/download_jmeter.cgi
2. Tải file `apache-jmeter-5.6.3.zip`
3. Giải nén vào thư mục (ví dụ: `C:\apache-jmeter`)
4. Thêm `C:\apache-jmeter\bin` vào PATH
5. Verify: `jmeter --version`

### Linux:
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install default-jdk
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
sudo mv apache-jmeter-5.6.3 /opt/jmeter
echo 'export PATH=$PATH:/opt/jmeter/bin' >> ~/.bashrc
source ~/.bashrc
```

---

## Bước 2: Chuẩn bị Test Accounts

### Option A: Tự động với Script (Recommended)

```bash
# 1. Start backend với PERF_MODE
make dev-perf

# 2. Chạy script tạo accounts (terminal mới)
cd performance-tests
./create-test-accounts.sh

# 3. Thêm số dư vào accounts
docker exec -i ezwallet-postgres psql -U ezwallet -d ezwallet < add-balance-to-test-accounts.sql
```

### Option B: Tạo thủ công qua Frontend

```bash
# 1. Start ứng dụng
make dev-perf

# 2. Mở browser: http://localhost:4200

# 3. Đăng ký 5 accounts:
#    - user1@test.com / Password123!
#    - user2@test.com / Password123!
#    - user3@test.com / Password123!
#    - user4@test.com / Password123!
#    - user5@test.com / Password123!

# 4. Với mỗi account:
#    - Điền form đăng ký (họ tên, email, SĐT, password)
#    - Check MailHog để lấy OTP: http://localhost:8025
#    - Xác thực account
#    - Login và top-up ít nhất 10,000,000 VND
```

### Verify Accounts:
```bash
# Check trong PostgreSQL
docker exec -it ezwallet-postgres psql -U ezwallet -d ezwallet

# Chạy query
SELECT u.email, u.status, w.balance 
FROM users u 
LEFT JOIN wallets w ON u.id = w.user_id 
WHERE u.email LIKE 'user%@test.com';
```

Kết quả phải thấy 5 accounts với `status = ACTIVE` và `balance > 10000000`.

---

## Bước 3: Start Backend với PERF_MODE

**QUAN TRỌNG:** Phải start backend ở PERF_MODE để OTP luôn là `123456`

```bash
# Start full stack với PERF_MODE
make dev-perf
```

Verify PERF_MODE đang active:
- Check backend logs: Phải thấy `⚠️  PERF_MODE=true - All OTPs will be 123456`
- Khi request OTP, backend log sẽ in: `PERF MODE: Generated fixed OTP for user...`

---

## Bước 4: Chạy Test với JMeter GUI

JMeter GUI mode dùng để **test/debug** test plan, KHÔNG dùng để chạy performance test thật.

### 4.1. Mở JMeter GUI

```bash
cd performance-tests/jmeter/testplans
jmeter
```

### 4.2. Mở Test Plan

1. Trong JMeter GUI, click **File → Open**
2. Chọn file test plan:
   - `Transfer-Performance-Test.jmx` (P2P Transfer)
   - `QR-Performance-Test.jmx` (QR Payment)
   - `Transaction-History-Performance-Test.jmx` (Transaction History)

### 4.3. Kiểm tra Configuration

Trong Test Plan tree, click vào **Test Plan**:
- `BASE_URL`: `http://localhost:8080/api/v1` ✓
- `THREADS`: Số lượng users đồng thời (mặc định 50)
- `RAMP_UP`: Thời gian tăng dần users (mặc định 30s)
- `DURATION`: Tổng thời gian test (mặc định 300s = 5 phút)

Bạn có thể điều chỉnh các giá trị này ở **User Defined Variables**.

### 4.4. Kiểm tra Test Data

Click vào **CSV Data Set - Users**:
- File: `../testdata/users.csv`
- Verify file path đúng

### 4.5. Chạy Test (Nhẹ để test)

**QUAN TRỌNG:** Trong GUI mode, GIẢM số threads xuống để test:

1. Click **Test Plan** → sửa `THREADS` = `5` (thay vì 50)
2. Click **Run → Start** (hoặc Ctrl+R / Cmd+R)
3. Click vào **View Results Tree** để xem từng request
4. Click vào **Summary Report** để xem tổng quan

### 4.6. Verify Test đang chạy đúng

Check các listener:
- **View Results Tree**: Tất cả requests phải màu xanh (HTTP 200/201)
- **Summary Report**: Error % phải = 0%
- Nếu có lỗi đỏ → click vào request để xem error details

### 4.7. Stop Test

Click **Run → Stop** (hoặc Ctrl+.)

---

## Bước 5: Chạy Test với JMeter CLI (Production Mode)

CLI mode dùng để chạy **performance test thật** với kết quả chính xác.

### 5.1. Chạy một Test Plan

```bash
cd performance-tests/jmeter/testplans

# Test Transfer Module
jmeter -n -t Transfer-Performance-Test.jmx \
  -l ../results/transfer-results.jtl \
  -e -o ../results/transfer-html-report

# Test QR Module
jmeter -n -t QR-Performance-Test.jmx \
  -l ../results/qr-results.jtl \
  -e -o ../results/qr-html-report

# Test Transaction History
jmeter -n -t Transaction-History-Performance-Test.jmx \
  -l ../results/transaction-history-results.jtl \
  -e -o ../results/transaction-history-html-report
```

**Giải thích tham số:**
- `-n`: Non-GUI mode (CLI)
- `-t`: Test plan file
- `-l`: Log file output (.jtl format)
- `-e -o`: Generate HTML dashboard report

### 5.2. Chạy với Custom Parameters

```bash
# Tăng số threads lên 100, chạy 10 phút
jmeter -n -t Transfer-Performance-Test.jmx \
  -JTHREADS=100 \
  -JRAMP_UP=60 \
  -JDURATION=600 \
  -l ../results/transfer-high-load.jtl \
  -e -o ../results/transfer-high-load-report
```

### 5.3. Chạy TẤT CẢ Tests (Tự động)

```bash
cd performance-tests/jmeter

# Chạy script tự động
./run-all-tests.sh

# Hoặc với custom parameters
./run-all-tests.sh 100 60 600
# (100 threads, 60s ramp-up, 600s duration)
```

Script sẽ:
1. Chạy Transfer test → đợi 30s
2. Chạy QR test → đợi 30s
3. Chạy Transaction History test
4. Generate HTML reports cho cả 3

### 5.4. Monitor Test Progress

Trong khi test chạy:

```bash
# Terminal 1: Monitor backend logs
cd backend && tail -f logs/spring.log

# Terminal 2: Monitor database
docker exec -it ezwallet-postgres psql -U postgres -d ezwallet \
  -c "SELECT count(*) FROM transactions;"

# Terminal 3: Check system resources
htop  # hoặc Activity Monitor trên macOS
```

---

## Bước 6: Xem và Phân tích Kết quả

### 6.1. Mở HTML Report

```bash
cd performance-tests/jmeter/results

# macOS
open transfer-html-report/index.html

# Windows
start transfer-html-report/index.html

# Linux
xdg-open transfer-html-report/index.html
```

### 6.2. Các Metrics Quan Trọng

**Dashboard Overview:**
- **Throughput**: Requests per second (RPS)
  - Target: > 100 RPS
- **Errors**: Percentage of failed requests
  - Target: < 1%
- **Response Time**: 
  - Average: < 200ms
  - 90th Percentile: < 300ms
  - 95th Percentile: < 500ms
  - 99th Percentile: < 1000ms

**Charts to Check:**
1. **Response Times Over Time**: Nên stable, không tăng theo thời gian
2. **Active Threads Over Time**: Kiểm tra ramp-up đúng pattern
3. **Response Times Percentiles**: p90, p95, p99 trong ngưỡng acceptable
4. **Transactions Per Second**: Throughput ổn định
5. **Response Time vs Request**: Không có correlation (tốt)

### 6.3. Phân tích Chi tiết

**Statistics Table (trong report):**
```
Label               # Samples   Average   90%ile   95%ile   99%ile   Error%   Throughput
Login               5000        120ms     180ms    220ms    350ms    0.0%     83.3/sec
Request OTP         4500        95ms      150ms    180ms    280ms    0.0%     75.0/sec
P2P Transfer        4200        250ms     400ms    500ms    800ms    0.2%     70.0/sec
```

**Red Flags (Cần chú ý):**
- ❌ Error rate > 5%
- ❌ Response time tăng liên tục theo thời gian
- ❌ p95 > 1000ms
- ❌ Throughput giảm dần

### 6.4. Export Results

```bash
# Raw data (.jtl file) có thể import lại vào JMeter
# hoặc process với tools khác

# View raw data
head -20 results/transfer-results.jtl
```

---

## 🔧 Troubleshooting

### Lỗi: "Connection refused"
```
❌ java.net.ConnectException: Connection refused
```
**Fix:** Backend chưa chạy → `make dev-perf`

### Lỗi: "401 Unauthorized"
```
❌ HTTP 401 - Unauthorized
```
**Fix:** 
- Check users.csv có đúng credentials không
- Verify accounts đã được tạo và activated

### Lỗi: "OTP_INVALID"
```
❌ BusinessException: OTP_INVALID
```
**Fix:** Backend KHÔNG chạy ở PERF_MODE
- Stop backend
- Start lại: `make dev-perf`
- Verify log có "⚠️ PERF_MODE=true"

### Lỗi: "INSUFFICIENT_BALANCE"
```
❌ BusinessException: INSUFFICIENT_BALANCE
```
**Fix:** Accounts không có đủ tiền
```bash
docker exec -i ezwallet-postgres psql -U postgres -d ezwallet < \
  performance-tests/add-balance-to-test-accounts.sql
```

### High Error Rate (> 10%)
**Possible causes:**
1. Database connection pool exhausted
2. OOM (Out of Memory)
3. Too many threads (giảm THREADS xuống)
4. Network issues

**Fix:**
- Giảm số threads: `-JTHREADS=30`
- Tăng ramp-up: `-JRAMP_UP=60`
- Check backend logs: `cd backend && tail -f logs/spring.log`

---

## 📊 Example Test Run

```bash
# Complete workflow
cd /path/to/EzWallet

# 1. Start infrastructure
make dev-perf

# 2. Create test accounts (nếu chưa có)
cd performance-tests
./create-test-accounts.sh
docker exec -i ezwallet-postgres psql -U postgres -d ezwallet < add-balance-to-test-accounts.sql

# 3. Run all tests
cd jmeter
./run-all-tests.sh

# 4. Open results
open results/transfer-html-*/index.html
open results/qr-html-*/index.html
open results/transaction-history-html-*/index.html
```

**Expected Results:**
- Throughput: 80-120 RPS per module
- Error Rate: 0-1%
- Average Response Time: 150-300ms
- p95 Response Time: < 500ms

---

## 🎓 Tips & Best Practices

1. **Always run tests in CLI mode** for accurate results
2. **Warm up the system** - Ignore first 30-60s of data
3. **Run multiple iterations** - Results vary between runs
4. **Monitor system resources** - CPU, memory, disk I/O
5. **Test one module at a time** initially
6. **Gradually increase load** - Start with 10 threads, then 50, 100, etc.
7. **Keep test data fresh** - Reset database between major test runs

---

## 📚 Resources

- JMeter Documentation: https://jmeter.apache.org/usermanual/
- Performance Testing Best Practices: https://jmeter.apache.org/usermanual/best-practices.html
- EzWallet Performance Tests: `performance-tests/README.md`
