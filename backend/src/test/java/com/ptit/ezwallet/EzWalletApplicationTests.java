package com.ptit.ezwallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test - đảm bảo Spring context load được với profile test.
 */
@SpringBootTest
@ActiveProfiles("test")
class EzWalletApplicationTests {

    @Test
    void contextLoads() {
        // Test rỗng có chủ đích: Spring tự fail nếu context load không thành công.
    }
}
