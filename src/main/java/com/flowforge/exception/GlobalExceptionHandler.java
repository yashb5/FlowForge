package com.flowforge.exception;

import com.flowforge.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("RESOURCE_NOT_FOUND")
                .field(ex.getFieldName())
                .rejectedValue(ex.getFieldValue())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("DUPLICATE_RESOURCE")
                .field(ex.getFieldName())
                .rejectedValue(ex.getFieldValue())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            ValidationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("VALIDATION_ERROR")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStateException(
            InvalidStateException ex, WebRequest request) {
        log.warn("Invalid state: {}", ex.getMessage());
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("INVALID_STATE")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("AUTHENTICATION_FAILED")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ACCESS_DENIED")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied", error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed: {}", ex.getMessage());
        
        List<ApiResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ApiResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("VALIDATION_ERROR")
                .fieldErrors(fieldErrors)
                .build();
        
        String message = fieldErrors.stream()
                .map(fe -> fe.getField() + ": " + fe.getMessage())
                .collect(Collectors.joining(", "));
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed: " + message, error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        
        List<ApiResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> ApiResponse.FieldError.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("CONSTRAINT_VIOLATION")
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", error));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Type mismatch: {}", ex.getMessage());
        
        String message = String.format("Parameter '%s' should be of type %s", 
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("TYPE_MISMATCH")
                .field(ex.getName())
                .rejectedValue(ex.getValue())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message, error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("ILLEGAL_ARGUMENT")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUncaughtException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        
        ApiResponse.ErrorDetails error = ApiResponse.ErrorDetails.builder()
                .code("INTERNAL_ERROR")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later.", error));
    }
}
