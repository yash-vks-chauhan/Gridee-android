package com.parking.app.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Tracing and Observability Configuration
 * Enables AOP-based tracing with Micrometer
 */
@Configuration
@EnableAspectJAutoProxy
public class TracingConfig {

    /**
     * Enable @Observed annotation support for method-level tracing
     */
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }
}

