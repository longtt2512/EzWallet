# JMeter Test Plans - Endpoint & Payload Verification

## ✅ Test Plans Overview

### 1. Transfer-Performance-Test.jmx

| Step | Endpoint | Method | Payload | Status |
|------|----------|--------|---------|--------|
| Setup: Login | `/api/v1/auth/login` | POST | `{"identifier": "${identifier}", "password": "${password}"}` | ✅ Fixed |
| Step 1: Request OTP | `/api/v1/transfer/otp` | POST | (empty body) | ⚠️ Need to verify |
| Step 2: P2P Transfer | `/api/v1/transfer/p2p` | POST | `{"recipientPhone": "...", "amount": ..., "note": "...", "otp": "123456"}` | ⚠️ Need to verify |

### 2. QR-Performance-Test.jmx

| Step | Endpoint | Method | Payload | Status |
|------|----------|--------|---------|--------|
| Setup: Login | `/api/v1/auth/login` | POST | `{"identifier": "${identifier}", "password": "${password}"}` | ✅ Fixed |
| Step 1: Generate QR | `/api/v1/transfer/qr/generate` | POST | `{"amount": ..., "note": "..."}` | ⚠️ Need to verify |
| Step 2: Get QR Info | `/api/v1/transfer/qr/${qrId}` | GET | - | ⚠️ Need to verify |
| Step 3: Request OTP | `/api/v1/transfer/otp` | POST | (empty body) | ⚠️ Need to verify |
| Step 4: Pay with QR | `/api/v1/transfer/qr/pay` | POST | `{"qrId": "...", "otp": "123456"}` | ⚠️ Need to verify |

### 3. Transaction-History-Performance-Test.jmx

| Step | Endpoint | Method | Query Params | Status |
|------|----------|--------|--------------|--------|
| Setup: Login | `/api/v1/auth/login` | POST | `{"identifier": "${identifier}", "password": "${password}"}` | ✅ Fixed |
| Scenario 1 | `/api/v1/transactions` | GET | `?page=0&size=20` | ⚠️ Need to verify |
| Scenario 2 | `/api/v1/transactions` | GET | `?type=TRANSFER&page=0&size=20` | ⚠️ Need to verify |
| Scenario 3 | `/api/v1/transactions` | GET | `?status=SUCCESS&page=0&size=20` | ⚠️ Need to verify |

## 🔍 Issues Found

### Issue 1: Login Fixed ✅
- **Problem**: Was using `"email": "${email}"`
- **Fixed**: Now using `"identifier": "${identifier}"`
- **CSV Data**: Updated to use `identifier` column instead of `email`

### Issue 2: Accounts Not Verified ⚠️
**Error**: `ACCOUNT_NOT_VERIFIED` when trying to login
**Root Cause**: Script created accounts but they need verification

**Need to verify in database:**
```sql
SELECT username, email, status FROM users WHERE username LIKE 'user%';
```

**Expected**: All accounts should have `status = 'ACTIVE'`

## 🔧 Action Items

### 1. Verify Account Status
```bash
docker exec ezwallet-postgres psql -U ezwallet -d ezwallet -c \
  "SELECT username, email, status FROM users WHERE username LIKE 'user%' ORDER BY username;"
```

### 2. Verify Endpoints Exist in Backend

Need to check these controllers:
- `TransferController` - for `/transfer/otp`, `/transfer/p2p`, `/transfer/qr/*`
- `TransactionController` - for `/transactions`

### 3. Test Each Endpoint Manually

**Login:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier": "user1", "password": "Password123!"}'
```

**Request OTP:**
```bash
TOKEN="<access_token_from_login>"
curl -X POST http://localhost:8080/api/v1/transfer/otp \
  -H "Authorization: Bearer $TOKEN"
```

**Get Transactions:**
```bash
TOKEN="<access_token_from_login>"
curl -X GET "http://localhost:8080/api/v1/transactions?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

## 📋 Next Steps

1. ✅ Fix login payload - DONE
2. ⏳ Verify accounts are created and ACTIVE
3. ⏳ Verify all transfer endpoints exist
4. ⏳ Verify all transaction endpoints exist  
5. ⏳ Test complete flow manually
6. ⏳ Run JMeter GUI test with 1 thread
7. ⏳ Run full JMeter CLI test

## 🎯 Current Status

**What's Working:**
- ✅ JMeter test plans structure
- ✅ Login payload format (identifier/password)
- ✅ CSV data format
- ✅ PERF_MODE for fixed OTP

**What Needs Verification:**
- ⚠️ Account creation and verification status
- ⚠️ Transfer API endpoints structure
- ⚠️ Transaction API endpoints structure
- ⚠️ Request/response formats match between JMeter and backend
