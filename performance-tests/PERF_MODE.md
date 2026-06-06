# Performance Testing Guide - OTP Mode

## PERF_MODE Implementation

The backend now supports a special performance testing mode that generates fixed OTP codes for reliable load testing.

### How it works

**OtpService.java** changes:
```java
@Value("${ezwallet.perf-mode:false}")
private boolean perfMode;

public String generate(User user, String contact, OtpPurpose purpose) {
    String code;
    if (perfMode) {
        code = "123456";  // Fixed OTP for performance testing
        log.debug("PERF MODE: Generated fixed OTP for user  and purpose {}", 
                  user.getEmail(), purpose);
    } else {
        code = String.format("%06d", RANDOM.nextInt(1_000_000));  // Random OTP
    }
    // ... rest of the method
}
```

**application.yml** changes:
```yaml
ezwallet:
  perf-mode: ${PERF_MODE:false}  # Read from environment variable
```

### Usage

#### Option 1: Use make command (Recommended)
```bash
# Start full dev environment with perf mode
make dev-perf

# Or just backend with perf mode
make be-run-perf
```

#### Option 2: Manual environment variable
```bash
export PERF_MODE=true
cd backend
./gradlew bootRun
```

#### Option 3: Temporary for one command
```bash
PERF_MODE=true ./gradlew bootRun
```

### Verification

Check backend logs on startup - you should see:
```
PERF MODE: Generated fixed OTP for user xxx and purpose TRANSFER
```

### Security Warning

**⚠️ NEVER enable PERF_MODE in production environments!**

This mode is designed exclusively for load testing and would be a serious security vulnerability in production. The default value is `false` and it only activates via environment variable.

### JMeter Integration

All JMeter test plans are configured to use OTP = `123456`, which matches the PERF_MODE output:

```xml
<!-- In all test plans -->
<stringProp name="Argument.value">{
  "otp": "123456",
  ...
}</stringProp>
```

### Test Flow

1. Start infrastructure and backend: `make dev-perf`
2. Verify PERF_MODE is active (check logs for "PERF MODE: Generated fixed OTP")
3. Run JMeter tests: `cd performance-tests/jmeter && ./run-all-tests.sh`
4. All OTP validations will succeed with `123456`
