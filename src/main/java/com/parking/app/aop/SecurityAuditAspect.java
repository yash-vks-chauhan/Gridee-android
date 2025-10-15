package com.parking.app.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security Audit Aspect - Logs all security-related events for audit trail
 */
@Aspect
@Component
public class SecurityAuditAspect {

    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final String USER_ID_KEY = "userId";

    /**
     * Pointcut for authentication endpoints
     */
    @Pointcut("execution(* com.parking.app.controller.AuthController.*(..))")
    public void authOperations() {}

    /**
     * Pointcut for payment operations
     */
    @Pointcut("execution(* com.parking.app.service.PaymentGatewayService.*(..))")
    public void paymentOperations() {}

    /**
     * Pointcut for wallet operations
     */
    @Pointcut("execution(* com.parking.app.service.WalletService.*(..))")
    public void walletOperations() {}

    /**
     * Pointcut for booking operations
     */
    @Pointcut("execution(* com.parking.app.service.BookingService.*(..))")
    public void bookingOperations() {}

    /**
     * Log all authentication attempts
     */
    @Before("authOperations()")
    public void auditAuthOperation() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = extractUserId(auth);

        MDC.put(USER_ID_KEY, userId);
        auditLogger.info("🔐 AUTH_OPERATION: userId={}", userId);
    }

    /**
     * Log all payment operations
     */
    @Before("paymentOperations()")
    public void auditPaymentOperation() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = extractUserId(auth);

        MDC.put(USER_ID_KEY, userId);
        auditLogger.info("💳 PAYMENT_OPERATION: userId={}", userId);
    }

    /**
     * Log all wallet operations
     */
    @Before("walletOperations()")
    public void auditWalletOperation() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = extractUserId(auth);

        MDC.put(USER_ID_KEY, userId);
        auditLogger.info("💰 WALLET_OPERATION: userId={}", userId);
    }

    /**
     * Log all booking operations
     */
    @Before("bookingOperations()")
    public void auditBookingOperation() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = extractUserId(auth);

        MDC.put(USER_ID_KEY, userId);
        auditLogger.info("📅 BOOKING_OPERATION: userId={}", userId);
    }

    /**
     * Extract user ID from authentication
     */
    private String extractUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() != null) {
            if (auth.getPrincipal() instanceof com.parking.app.model.Users) {
                return ((com.parking.app.model.Users) auth.getPrincipal()).getId();
            }
            return auth.getName();
        }
        return "anonymous";
    }
}

