package com.parking.app.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

/**
 * AOP Aspect for comprehensive method execution logging and tracing
 * Logs entry, exit, exceptions, and execution time for all service and controller methods
 */
@Aspect
@Component
public class LoggingAspect {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String METHOD_KEY = "method";
    private static final String CLASS_KEY = "class";

    /**
     * Pointcut for all methods in service packages
     */
    @Pointcut("execution(* com.parking.app.service..*(..))")
    public void serviceLayer() {}

    /**
     * Pointcut for all methods in controller packages
     */
    @Pointcut("execution(* com.parking.app.controller..*(..))")
    public void controllerLayer() {}

    /**
     * Pointcut for all methods in repository packages
     */
    @Pointcut("execution(* com.parking.app.repository..*(..))")
    public void repositoryLayer() {}

    /**
     * Around advice for service layer - logs entry, exit, execution time, and exceptions
     */
    @Around("serviceLayer()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "SERVICE");
    }

    /**
     * Around advice for controller layer - logs entry, exit, execution time, and exceptions
     */
    @Around("controllerLayer()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "CONTROLLER");
    }

    /**
     * Around advice for repository layer - logs entry, exit, execution time, and exceptions
     */
    @Around("repositoryLayer()")
    public Object logRepositoryExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint, "REPOSITORY");
    }

    /**
     * Common method execution logging logic
     */
    private Object logMethodExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String traceId = MDC.get(TRACE_ID_KEY);

        // Generate trace ID if not present
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put(TRACE_ID_KEY, traceId);
        }

        // Add method context to MDC
        MDC.put(CLASS_KEY, className);
        MDC.put(METHOD_KEY, methodName);

        long startTime = System.currentTimeMillis();

        try {
            // Log method entry with arguments
            if (logger.isDebugEnabled()) {
                logger.debug("[{}] → ENTER {}.{} with args: {}",
                    layer, className, methodName, Arrays.toString(joinPoint.getArgs()));
            } else {
                logger.info("[{}] → ENTER {}.{}", layer, className, methodName);
            }

            // Execute the actual method
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // Log method exit with execution time
            if (logger.isDebugEnabled()) {
                logger.debug("[{}] ← EXIT {}.{} ({}ms) with result: {}",
                    layer, className, methodName, executionTime, result);
            } else {
                logger.info("[{}] ← EXIT {}.{} ({}ms)",
                    layer, className, methodName, executionTime);
            }

            // Log slow executions as warnings
            if (executionTime > 1000) {
                logger.warn("[{}] SLOW EXECUTION: {}.{} took {}ms",
                    layer, className, methodName, executionTime);
            }

            return result;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log exception with full stack trace
            logger.error("[{}] ✗ EXCEPTION in {}.{} ({}ms): {} - {}",
                layer, className, methodName, executionTime,
                e.getClass().getSimpleName(), e.getMessage(), e);

            throw e;

        } finally {
            // Clean up MDC
            MDC.remove(CLASS_KEY);
            MDC.remove(METHOD_KEY);
        }
    }

    /**
     * After throwing advice - logs all exceptions with context
     */
    @AfterThrowing(pointcut = "serviceLayer() || controllerLayer() || repositoryLayer()",
                   throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringTypeName();

        logger.error("Exception in {}.{}: {}",
            className, methodName, exception.getMessage());
    }
}

