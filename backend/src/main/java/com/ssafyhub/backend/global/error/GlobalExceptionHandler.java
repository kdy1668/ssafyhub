package com.ssafyhub.backend.global.error;

import com.ssafyhub.backend.global.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        log.warn(
                "Business exception occurred: code={}, status={}",
                errorCode.getCode(),
                errorCode.getStatus()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationErrorResponse>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        List<ValidationErrorResponse> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();

        log.warn("Request validation failed: errorCount={}", errors.size());

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage(), errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<ValidationErrorResponse>>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        List<ValidationErrorResponse> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ValidationErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        log.warn("Constraint validation failed: errorCount={}", errors.size());

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage(), errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception
    ) {
        log.warn("Request body is not readable: {}", exception.getMessage());

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unexpected exception occurred: type={}", exception.getClass().getName());

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }
}
