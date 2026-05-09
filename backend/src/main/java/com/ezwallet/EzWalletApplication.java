package com.ezwallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry-point ứng dụng EzWallet.
 *
 * <p>Bật sẵn:
 * <ul>
 *   <li>JPA Auditing để tự gán createdAt / updatedAt cho entity.</li>
 *   <li>Async để gửi email/OTP không block request hiện tại.</li>
 *   <li>Scheduling để job định kỳ (ví dụ huỷ giao dịch PENDING quá hạn).</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class EzWalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(EzWalletApplication.class, args);
    }
}
