package com.parking.app.config;

import com.parking.app.exception.ConflictException;
import com.parking.app.exception.InsufficientFundsException;
import com.parking.app.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler using @RestControllerAdvice
 * Follows RFC 7807 (Problem Details for HTTP APIs) standard
 * Compatible with industry standards: REST API Best Practices, OpenAPI 3.0
 *
 * Features:
 * - Standardized error response format (RFC 7807)
 * - Distributed tracing integration (trace ID in responses)
 * - Comprehensive exception coverage
 * - Detailed validation error reporting
 * - Security-aware error messages (no sensitive data leakage)
 * - Structured logging with MDC context
 */
@RestControllerAdvice
public class GlobalExceptionControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionControllerAdvice.class);
    private static final String ERROR_LOG_FORMAT = "Error handled | status={} error_type={} error_code={} trace_id={} path={}";

    // ==================== Business Logic Exceptions ====================

    /**
     * Handle NotFoundException (404)
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiErrorResponse> handleNotFoundException(
            NotFoundException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    /**
     * Handle ConflictException (409)
     */
    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiErrorResponse> handleConflictException(
            ConflictException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "RESOURCE_CONFLICT",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    /**
     * Handle IllegalStateException (400)
     */
    @ExceptionHandler(com.parking.app.exception.IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(
            com.parking.app.exception.IllegalStateException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "ILLEGAL_STATE",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    /**
     * Handle InsufficientFundsException (402)
     */
    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ResponseEntity<ApiErrorResponse> handleInsufficientFundsException(
            InsufficientFundsException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.PAYMENT_REQUIRED,
                "INSUFFICIENT_FUNDS",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    // ==================== Validation Exceptions ====================

    /**
     * Handle validation errors from @Valid annotations (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        String message = String.format("Validation failed for %d field(s)", validationErrors.size());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                request.getRequestURI(),
                validationErrors
        );
    }

    /**
     * Handle constraint violation exceptions (400)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> validationErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing
                ));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "CONSTRAINT_VIOLATION",
                "Validation constraint violated",
                request.getRequestURI(),
                validationErrors
        );
    }

    /**
     * Handle missing request parameters (400)
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String message = String.format("Required parameter '%s' of type '%s' is missing",
                ex.getParameterName(), ex.getParameterType());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                message,
                request.getRequestURI(),
                null
        );
    }

    /**
     * Handle type mismatch errors (400)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "TYPE_MISMATCH",
                message,
                request.getRequestURI(),
                null
        );
    }

    /**
     * Handle malformed JSON requests (400)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Request body is malformed or cannot be parsed",
                request.getRequestURI(),
                null
        );
    }

    // ==================== Security Exceptions ====================

    /**
     * Handle authentication failures (401)
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class, InsufficientAuthenticationException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            Exception ex, HttpServletRequest request) {

        // Add security event to MDC for audit
        MDC.put("securityEvent", "AUTHENTICATION_FAILED");
        MDC.put("sourceIp", getClientIp(request));

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED",
                "Authentication failed. Please provide valid credentials.",
                request.getRequestURI(),
                null
        );
    }

    /**
     * Handle authorization failures (403)
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        // Add security event to MDC for audit
        MDC.put("securityEvent", "ACCESS_DENIED");
        MDC.put("sourceIp", getClientIp(request));

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "You don't have permission to access this resource.",
                request.getRequestURI(),
                null
        );
    }

    // ==================== HTTP Protocol Exceptions ====================

    /**
     * Handle unsupported HTTP methods (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String message = String.format("HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());

        return buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                message,
                request.getRequestURI(),
                null
        );
    }

    /**
     * Handle unsupported media types (415)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        String message = String.format("Media type '%s' is not supported. Supported types: %s",
                ex.getContentType(), ex.getSupportedMediaTypes());

        return buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                message,
                request.getRequestURI(),
                null
        );
    }

    /**
     * Handle 404 - No handler found (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {

        String message = String.format("Endpoint '%s %s' not found",
                ex.getHttpMethod(), ex.getRequestURL());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "ENDPOINT_NOT_FOUND",
                message,
                request.getRequestURI(),
                null
        );
    }

    // ==================== Generic Exception Handler ====================

    /**
     * Handle all other unhandled exceptions (500)
     * This is the catch-all handler
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        // Log full stack trace for debugging
        logger.error("Unhandled exception occurred | trace_id={} path={} error_type={}",
                MDC.get("traceId"), request.getRequestURI(), ex.getClass().getSimpleName(), ex);

        // Don't expose internal error details to clients in production
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI(),
                null
        );
    }

    // ==================== Helper Methods ====================

    /**
     * Build standardized error response following RFC 7807
     */
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            String path,
            Map<String, String> validationErrors) {

        String traceId = MDC.get("traceId");
        String requestId = MDC.get("requestId");

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .traceId(traceId)
                .requestId(requestId)
                .validationErrors(validationErrors)
                .build();

        // Structured logging
        logger.error(ERROR_LOG_FORMAT, status.value(), errorCode, errorCode, traceId, path);

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Extract client IP from request (handles proxies)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    /**
     * Standardized API Error Response (RFC 7807 compliant)
     * Compatible with: OpenAPI 3.0, REST API standards, Cloud-native applications
     */
    public static class ApiErrorResponse {
        private Instant timestamp;
        private int status;
        private String error;
        private String errorCode;
        private String message;
        private String path;
        private String traceId;
        private String requestId;
        private Map<String, String> validationErrors;

        // Builder pattern for flexible construction
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final ApiErrorResponse response = new ApiErrorResponse();

            public Builder timestamp(Instant timestamp) {
                response.timestamp = timestamp;
                return this;
            }

            public Builder status(int status) {
                response.status = status;
                return this;
            }

            public Builder error(String error) {
                response.error = error;
                return this;
            }

            public Builder errorCode(String errorCode) {
                response.errorCode = errorCode;
                return this;
            }

            public Builder message(String message) {
                response.message = message;
                return this;
            }

            public Builder path(String path) {
                response.path = path;
                return this;
            }

            public Builder traceId(String traceId) {
                response.traceId = traceId;
                return this;
            }

            public Builder requestId(String requestId) {
                response.requestId = requestId;
                return this;
            }

            public Builder validationErrors(Map<String, String> validationErrors) {
                response.validationErrors = validationErrors;
                return this;
            }

            public ApiErrorResponse build() {
                return response;
            }
        }

        // Getters and Setters
        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public Map<String, String> getValidationErrors() {
            return validationErrors;
        }

        public void setValidationErrors(Map<String, String> validationErrors) {
            this.validationErrors = validationErrors;
        }
    }
}

