// src/main/java/com/parking/app/config/SecurityConfig.java
package com.parking.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Disable HTML login page. We want JSON APIs only.
            .formLogin(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no auth)
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/users/register").permitAll()
                .requestMatchers("/api/users/social-signin").permitAll()
                .requestMatchers("/api/otp/**").permitAll()
                .requestMatchers("/api/oauth2/user").permitAll()  // Allow OAuth2 user endpoint for testing
                .requestMatchers("/api/payments/callback").permitAll()   // Allow payment callback without JWT
                // Everything else requires auth
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
