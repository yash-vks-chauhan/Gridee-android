package com.parking.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Filter to prevent brute force and DoS attacks
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    // Track requests per IP address
    private final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();

    // Configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final int MAX_LOGIN_ATTEMPTS_PER_MINUTE = 10;
    private static final long WINDOW_SIZE_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long CLEANUP_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

    private long lastCleanup = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String requestURI = request.getRequestURI();

        // Check rate limit
        if (isRateLimited(clientIp, requestURI)) {
            logger.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, requestURI);
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        // Track request
        trackRequest(clientIp, requestURI);

        // Cleanup old entries periodically
        cleanupIfNeeded();

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request should be rate limited
     */
    private boolean isRateLimited(String clientIp, String requestURI) {
        RateLimitEntry entry = requestCounts.get(clientIp);

        if (entry == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        // Reset if window expired
        if (now - entry.windowStart > WINDOW_SIZE_MS) {
            entry.reset(now);
            return false;
        }

        // Check login endpoint rate limit
        if (requestURI.contains("/api/auth/login")) {
            return entry.loginAttempts >= MAX_LOGIN_ATTEMPTS_PER_MINUTE;
        }

        // Check general rate limit
        return entry.requestCount >= MAX_REQUESTS_PER_MINUTE;
    }

    /**
     * Track the request for rate limiting
     */
    private void trackRequest(String clientIp, String requestURI) {
        long now = System.currentTimeMillis();

        requestCounts.compute(clientIp, (key, entry) -> {
            if (entry == null) {
                entry = new RateLimitEntry(now);
            } else if (now - entry.windowStart > WINDOW_SIZE_MS) {
                entry.reset(now);
            }

            entry.requestCount++;

            if (requestURI.contains("/api/auth/login")) {
                entry.loginAttempts++;
            }

            return entry;
        });
    }

    /**
     * Get client IP address, considering proxy headers
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

    /**
     * Cleanup old entries to prevent memory leak
     */
    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            requestCounts.entrySet().removeIf(entry ->
                now - entry.getValue().windowStart > WINDOW_SIZE_MS * 2
            );
            lastCleanup = now;
            logger.debug("Rate limit cache cleaned up. Current size: {}", requestCounts.size());
        }
    }

    /**
     * Rate limit tracking entry
     */
    private static class RateLimitEntry {
        long windowStart;
        int requestCount;
        int loginAttempts;

        RateLimitEntry(long windowStart) {
            this.windowStart = windowStart;
            this.requestCount = 0;
            this.loginAttempts = 0;
        }

        void reset(long newWindowStart) {
            this.windowStart = newWindowStart;
            this.requestCount = 0;
            this.loginAttempts = 0;
        }
    }
}

