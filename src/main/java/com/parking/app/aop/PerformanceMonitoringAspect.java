package com.parking.app.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Performance Monitoring Aspect - Tracks execution time with standardized metrics
 * Compatible with: Datadog APM, New Relic, Dynatrace, AWS X-Ray
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {

    private static final Logger perfLogger = LoggerFactory.getLogger("PERFORMANCE");
    private static final long SLOW_THRESHOLD_MS = 1000; // 1 second

    /**
     * Monitor all database operations
     */
    @Pointcut("execution(* com.parking.app.repository..*(..))")
    public void databaseOperations() {}

    /**
     * Monitor external API calls
     */
    @Pointcut("execution(* com.parking.app.service.PaymentGatewayService.*(..)) || " +
              "execution(* com.parking.app.service.OAuth2Service.*(..))")
    public void externalApiCalls() {}

    @Around("databaseOperations()")
    public Object monitorDatabasePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "database", "DB");
    }

    @Around("externalApiCalls()")
    public Object monitorApiPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "external_api", "API");
    }

    private Object monitorPerformance(ProceedingJoinPoint joinPoint, String resourceType, String type) throws Throwable {
        String operationName = joinPoint.getSignature().toShortString();
        String methodName = joinPoint.getSignature().getName();
        long startTime = System.currentTimeMillis();

        // Add performance context to MDC
        MDC.put("operationName", operationName);
        MDC.put("operationType", resourceType);
        MDC.put("resourceType", resourceType);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // Add metrics to MDC
            MDC.put("durationMs", String.valueOf(executionTime));
            MDC.put("thresholdExceeded", String.valueOf(executionTime > SLOW_THRESHOLD_MS));

            if (executionTime > SLOW_THRESHOLD_MS) {
                perfLogger.warn("SLOW_OPERATION | type={} operation={} duration_ms={} threshold_exceeded=true",
                    type, methodName, executionTime);
            } else {
                perfLogger.debug("OPERATION_COMPLETE | type={} operation={} duration_ms={}",
                    type, methodName, executionTime);
            }

            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            MDC.put("durationMs", String.valueOf(executionTime));
            MDC.put("errorType", e.getClass().getSimpleName());

            perfLogger.error("OPERATION_FAILED | type={} operation={} duration_ms={} error_type={} error_message={}",
                type, methodName, executionTime, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } finally {
            // Clean up MDC
            MDC.remove("operationName");
            MDC.remove("operationType");
            MDC.remove("resourceType");
            MDC.remove("durationMs");
            MDC.remove("thresholdExceeded");
            MDC.remove("errorType");
        }
    }
}
