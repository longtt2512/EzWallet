package com.ptit.ezwallet.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception khi không tìm thấy tài nguyên (user, ví, giao dịch, ...).
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, Object id) {
        super("NOT_FOUND",
              "Không tìm thấy " + resource + " với định danh: " + id,
              HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
