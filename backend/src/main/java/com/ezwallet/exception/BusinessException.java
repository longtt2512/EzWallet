package com.ezwallet.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception nghiệp vụ - dùng khi rule business bị vi phạm
 * (số dư không đủ, vượt hạn mức, OTP sai, ...).
 *
 * <p>GlobalExceptionHandler sẽ map sang response với mã code + HTTP status tương ứng.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
