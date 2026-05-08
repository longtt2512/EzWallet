package com.ptit.ezwallet.exception;

import com.ptit.ezwallet.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Bộ xử lý exception toàn cục.
 *
 * <p>Mục tiêu: client luôn nhận response có format thống nhất ({@link ApiResponse})
 * dù cho exception là gì - đảm bảo dễ kiểm thử (test biên, decision table)
 * và tránh leak stack trace ra ngoài.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Vi phạm rule nghiệp vụ tự định nghĩa */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("Business exception [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /** Validation thất bại trên @RequestBody */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_FAILED", "Dữ liệu không hợp lệ", errors));
    }

    /** Validation thất bại trên @PathVariable / @RequestParam */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        List<ApiResponse.FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> ApiResponse.FieldError.builder()
                        .field(v.getPropertyPath().toString())
                        .message(v.getMessage())
                        .build())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("VALIDATION_FAILED", "Tham số không hợp lệ", errors));
    }

    /** Vi phạm ràng buộc DB (unique, foreign key, ...) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DATA_CONFLICT", "Dữ liệu vi phạm ràng buộc"));
    }

    /** Sai user/pass */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("INVALID_CREDENTIALS", "Tên đăng nhập hoặc mật khẩu không đúng"));
    }

    /** Token thiếu / sai */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", "Bạn chưa đăng nhập hoặc phiên đã hết hạn"));
    }

    /** Không đủ quyền */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("ACCESS_DENIED", "Bạn không có quyền thực hiện hành động này"));
    }

    /** Mọi exception còn lại - fallback */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "Hệ thống đang gặp sự cố, vui lòng thử lại sau"));
    }

    private ApiResponse.FieldError toFieldError(FieldError fe) {
        return ApiResponse.FieldError.builder()
                .field(fe.getField())
                .message(fe.getDefaultMessage())
                .build();
    }
}
