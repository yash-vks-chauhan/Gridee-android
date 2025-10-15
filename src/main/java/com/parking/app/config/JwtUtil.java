package com.parking.app.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secretKey;

    // SECURITY FIX: Reduced token expiration to 1 day (was 7 days)
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000L; // 1 day

    // Token issuer for additional validation
    private static final String TOKEN_ISSUER = "gridee-parking-app";

    /**
     * SECURITY FIX: Stronger key generation using HMAC-SHA-256 with proper encoding
     */
    private Key getSigningKey() {
        try {
            // Decode base64 secret or use UTF-8 bytes with minimum length check
            byte[] keyBytes;
            if (secretKey.length() < 32) {
                logger.warn("JWT secret key is too short. Using padded key. Please use a stronger secret!");
                keyBytes = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
            } else {
                keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            logger.error("Failed to generate signing key: {}", e.getMessage());
            throw new IllegalStateException("JWT key generation failed", e);
        }
    }

    /**
     * Generate JWT token with enhanced security claims
     */
    public String generateToken(String userId, String role) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("tokenType", "access");

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuer(TOKEN_ISSUER)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * SECURITY FIX: Enhanced token parsing with proper exception handling
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .requireIssuer(TOKEN_ISSUER)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired: {}", e.getMessage());
            throw new SecurityException("Token has expired", e);
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token: {}", e.getMessage());
            throw new SecurityException("Unsupported token format", e);
        } catch (MalformedJwtException e) {
            logger.error("Malformed JWT token: {}", e.getMessage());
            throw new SecurityException("Invalid token format", e);
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
            throw new SecurityException("Invalid token signature", e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
            throw new SecurityException("Token is empty", e);
        }
    }

    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) extractClaims(token).get("role");
    }

    /**
     * SECURITY FIX: Enhanced token validation with issuer check
     */
    public boolean validateToken(String token, String userId) {
        try {
            final String extractedUserId = extractUserId(token);
            final String issuer = extractClaims(token).getIssuer();

            return (extractedUserId.equals(userId)
                    && !isTokenExpired(token)
                    && TOKEN_ISSUER.equals(issuer));
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get token expiration time in milliseconds
     */
    public long getTokenExpirationTime(String token) {
        try {
            return extractClaims(token).getExpiration().getTime();
        } catch (Exception e) {
            return 0;
        }
    }
}
