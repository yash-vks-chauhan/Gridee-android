package com.parking.app.config;

import com.parking.app.constants.SecurityConstants;
import com.parking.app.model.Users;
import com.parking.app.repository.UserRepository;
import com.parking.app.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * JWT Authentication Filter to validate and authenticate requests using JWT tokens.
 * This filter runs once per request and validates the JWT token from the Authorization header.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);

        // No Authorization header - continue without authentication
        if (authHeader == null || authHeader.trim().isEmpty()) {
            logger.debug("No Authorization header found for request: {} {}", method, requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // Validate Bearer token format
        if (!authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            logger.warn("Invalid Authorization header format for request: {} {}", method, requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(authHeader);

        if (token == null || token.trim().isEmpty()) {
            logger.warn("Empty token extracted from Authorization header");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticateRequest(request, token, requestURI);
        } catch (Exception e) {
            logger.error("Authentication failed for request: {} {} - Error: {}", method, requestURI, e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if the request URI matches any public endpoint pattern
     */
    private boolean isPublicEndpoint(String requestURI) {
        for (String pattern : SecurityConstants.PUBLIC_ENDPOINTS) {
            if (pathMatcher.match(pattern, requestURI)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractToken(String authHeader) {
        if (authHeader.length() <= SecurityConstants.BEARER_PREFIX_LENGTH) {
            return null;
        }
        return authHeader.substring(SecurityConstants.BEARER_PREFIX_LENGTH).trim();
    }

    /**
     * Authenticate the request using JWT token
     */
    private void authenticateRequest(HttpServletRequest request, String token, String requestURI) {
        // Check if token is blacklisted
        if (tokenBlacklistService.isBlacklisted(token)) {
            logger.warn("Attempt to use blacklisted token for URI: {}", requestURI);
            return;
        }

        // Extract user ID from token
        String userId = extractUserIdSafely(token);
        if (userId == null) {
            return;
        }

        // Skip if already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            logger.debug("Request already authenticated for user: {}", userId);
            return;
        }

        // Validate token
        if (!validateTokenSafely(token, userId)) {
            logger.warn("Invalid or expired token for user: {}", userId);
            return;
        }

        // Load user and set authentication
        Optional<Users> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            logger.error("User not found in database for userId: {}", userId);
            return;
        }

        Users user = userOptional.get();
        setAuthentication(request, user);
        logger.debug("Successfully authenticated user: {} for URI: {}", userId, requestURI);
    }

    /**
     * Safely extract user ID from token with exception handling
     */
    private String extractUserIdSafely(String token) {
        try {
            return jwtUtil.extractUserId(token);
        } catch (Exception e) {
            logger.error("Failed to extract userId from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Safely validate token with exception handling
     */
    private boolean validateTokenSafely(String token, String userId) {
        try {
            return jwtUtil.validateToken(token, userId);
        } catch (Exception e) {
            logger.error("Token validation failed for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Set authentication in SecurityContext
     */
    private void setAuthentication(HttpServletRequest request, Users user) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user,
                null,
                Collections.emptyList()
        );

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
