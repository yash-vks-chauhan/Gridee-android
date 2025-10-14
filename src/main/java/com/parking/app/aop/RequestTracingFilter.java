package com.parking.app.aop;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Request Tracing Filter - Adds trace ID and standardized context to every request
 * Following OpenTelemetry and ECS (Elastic Common Schema) standards
 * Compatible with: Datadog, New Relic, AWS CloudWatch, ELK Stack, Splunk
 */
@Component
@Order(1)
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestTracingFilter.class);

    // Standard header names (OpenTelemetry compatible)
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    // MDC keys following ECS standard
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String REQUEST_URI_KEY = "requestUri";
    private static final String REQUEST_METHOD_KEY = "requestMethod";
    private static final String HTTP_STATUS_CODE_KEY = "httpStatusCode";
    private static final String DURATION_MS_KEY = "durationMs";
    private static final String ERROR_TYPE_KEY = "errorType";
    private static final String SOURCE_IP_KEY = "sourceIp";
    private static final String USER_AGENT_KEY = "userAgent";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            // Get or generate trace ID (OpenTelemetry format)
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.trim().isEmpty()) {
                traceId = generateTraceId();
            }

            // Generate span ID for this request
            String spanId = generateSpanId();

            // Generate unique request ID
            String requestId = UUID.randomUUID().toString().substring(0, 8);

            // Get source IP (considering proxies)
            String sourceIp = getClientIp(request);

            // Get user agent
            String userAgent = request.getHeader("User-Agent");

            // Add all context to MDC (following ECS standard)
            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(SPAN_ID_KEY, spanId);
            MDC.put(REQUEST_ID_KEY, requestId);
            MDC.put(REQUEST_URI_KEY, request.getRequestURI());
            MDC.put(REQUEST_METHOD_KEY, request.getMethod());
            MDC.put(SOURCE_IP_KEY, sourceIp);
            if (userAgent != null) {
                MDC.put(USER_AGENT_KEY, userAgent);
            }

            // Add trace headers to response (for distributed tracing)
            response.setHeader(TRACE_ID_HEADER, traceId);
            response.setHeader(SPAN_ID_HEADER, spanId);

            // Log incoming request with standardized format
            logger.info("⊳ HTTP Request | method={} uri={} client_ip={} trace_id={} span_id={}",
                request.getMethod(),
                request.getRequestURI(),
                sourceIp,
                traceId,
                spanId);

            // Process the request
            filterChain.doFilter(request, response);

            // Calculate execution time
            long executionTime = System.currentTimeMillis() - startTime;

            // Add response metrics to MDC
            MDC.put(HTTP_STATUS_CODE_KEY, String.valueOf(response.getStatus()));
            MDC.put(DURATION_MS_KEY, String.valueOf(executionTime));

            // Log completed request with metrics
            logger.info("⊲ HTTP Response | method={} uri={} status={} duration_ms={} trace_id={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                executionTime,
                traceId);

            // Warn on slow requests (SLO: 2 seconds)
            if (executionTime > 2000) {
                logger.warn("⚠ SLOW_REQUEST | method={} uri={} duration_ms={} threshold_exceeded=true trace_id={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    executionTime,
                    traceId);
            }

        } catch (Exception e) {
            // Log error with context
            MDC.put(ERROR_TYPE_KEY, e.getClass().getSimpleName());
            logger.error("✗ HTTP Request Failed | method={} uri={} error_type={} error_message={}",
                request.getMethod(),
                request.getRequestURI(),
                e.getClass().getSimpleName(),
                e.getMessage(), e);
            throw e;
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }

    /**
     * Generate OpenTelemetry compatible trace ID (32 hex characters)
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "") +
               UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Generate OpenTelemetry compatible span ID (16 hex characters)
     */
    private String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Get client IP address, considering proxy headers (X-Forwarded-For, X-Real-IP)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Take first IP if comma-separated
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }
}
