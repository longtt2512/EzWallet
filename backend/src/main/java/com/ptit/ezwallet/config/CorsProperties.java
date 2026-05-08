package com.ptit.ezwallet.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cấu hình CORS từ application.yml (prefix: ezwallet.cors).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ezwallet.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:4200");
    private String allowedMethods = "GET,POST,PUT,DELETE,PATCH,OPTIONS";
    private String allowedHeaders = "*";
    private boolean allowCredentials = true;
}
