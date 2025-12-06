package com.parking.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration to enable Spring Retry mechanism
 * Allows automatic retry of failed operations due to transient errors
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry is now enabled for @Retryable annotated methods
}

