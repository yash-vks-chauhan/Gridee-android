package com.parking.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extremely simple in-memory rate limiter to protect public endpoints.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_REQUESTS = 120;

    private final Map<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = request.getRemoteAddr();
        if (isRateLimited(key)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"message\":\"Too many requests. Slow down.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key) {
        long now = Instant.now().toEpochMilli();
        Deque<Long> timestamps = requestLog.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > WINDOW_MILLIS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                return true;
            }
            timestamps.addLast(now);
        }
        return false;
    }
}
