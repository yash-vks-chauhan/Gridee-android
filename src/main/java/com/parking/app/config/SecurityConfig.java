package com.parking.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired(required = false)
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter, RateLimitingFilter rateLimitingFilter) throws Exception {

        // SECURITY FIX: Use custom CSRF handler that bypasses validation for JWT requests
        JwtCsrfTokenRequestHandler requestHandler = new JwtCsrfTokenRequestHandler();

        http
                .authorizeHttpRequests(auth -> auth
                        // Public read-only endpoints for mobile home screens
                        .requestMatchers(HttpMethod.GET, "/api/parking-spots/**", "/api/parking-lots/**").permitAll()
                        .requestMatchers(
                                "/api/parking-lots/list/by-names",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/users/social-signin",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // SECURITY FIX: Enable CSRF protection with custom handler for JWT bypass
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(requestHandler) // Use custom handler
                        .ignoringRequestMatchers("/api/**") // Only for stateless auth endpoints
                )
                // SECURITY FIX: Add security headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny()) // Prevent clickjacking
                        .xssProtection(xss -> xss.disable()) // Content-Security-Policy preferred
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data: https:; " +
                                        "font-src 'self' data:; " +
                                        "frame-ancestors 'none'")
                        )
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000) // 1 year
                        )
                )
                // SECURITY FIX: Stateless session management for JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // FILTER ORDER: Rate Limiting -> JWT Authentication
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // CONDITIONAL: Only configure OAuth2 if ClientRegistrationRepository is available
        if (clientRegistrationRepository != null && oAuth2LoginSuccessHandler != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .successHandler(oAuth2LoginSuccessHandler)
            );
        }

        return http.build();
    }
}
