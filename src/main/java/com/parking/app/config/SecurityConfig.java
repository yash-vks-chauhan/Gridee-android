package com.parking.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           RateLimitingFilter rateLimitingFilter) throws Exception {

        JwtCsrfTokenRequestHandler requestHandler = new JwtCsrfTokenRequestHandler();

        http
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers(csrfIgnoredPaths())
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(publicEndpoints()).permitAll()
                    .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .addFilterBefore(rateLimitingFilter, JwtAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private RequestMatcher[] csrfIgnoredPaths() {
        return new RequestMatcher[] {
                new AntPathRequestMatcher("/api/auth/**"),
                new AntPathRequestMatcher("/api/users/**"),
                new AntPathRequestMatcher("/swagger-ui/**"),
                new AntPathRequestMatcher("/v3/api-docs/**"),
                new AntPathRequestMatcher("/actuator/**"),
                new AntPathRequestMatcher("/error")
        };
    }

    private RequestMatcher[] publicEndpoints() {
        return new RequestMatcher[] {
                new AntPathRequestMatcher("/api/parking-lots/list/by-names", "GET"),
                new AntPathRequestMatcher("/api/auth/register", "POST"),
                new AntPathRequestMatcher("/api/auth/login", "POST"),
                new AntPathRequestMatcher("/api/users/register", "POST"),
                new AntPathRequestMatcher("/api/users/social-signin", "POST"),
                new AntPathRequestMatcher("/api/otp/**"),
                new AntPathRequestMatcher("/api/oauth2/user", "GET"),
                new AntPathRequestMatcher("/api/payments/callback", "POST"),
                new AntPathRequestMatcher("/swagger-ui/**"),
                new AntPathRequestMatcher("/v3/api-docs/**"),
                new AntPathRequestMatcher("/actuator/health"),
                new AntPathRequestMatcher("/error")
        };
    }
}
