# Performance Testing

This directory contains performance testing tools and documentation for EzWallet.

## Contents

- **jmeter/** - Apache JMeter test plans for load testing
  - Transfer Module (P2P transfers with OTP)
  - QR Payment Module (QR generation and payment)
  - Transaction History Module (filtering and pagination)

- **PERF_MODE.md** - Documentation about fixed OTP mode for testing

## Quick Start

```bash
# 1. Start infrastructure and backend in performance mode
make dev-perf

# 2. In another terminal, run JMeter tests
cd performance-tests/jmeter
./run-all-tests.sh
```

## What is PERF_MODE?

Performance mode enables fixed OTP generation (`123456`) to allow realistic load testing of OTP-protected endpoints without the overhead of email retrieval or OTP randomness.

**Security:** PERF_MODE defaults to `false` and only activates via environment variable. Never enable in production.

See [PERF_MODE.md](PERF_MODE.md) for detailed implementation.

## Test Results

JMeter generates HTML reports in `jmeter/results/` with:
- Response time graphs
- Throughput metrics
- Error rates
- Percentile statistics (p50, p90, p95, p99)

## Performance Targets

- **Response Time:** < 500ms (p95)
- **Throughput:** > 100 requests/second
- **Error Rate:** < 1%

See `jmeter/README.md` for detailed testing instructions.
