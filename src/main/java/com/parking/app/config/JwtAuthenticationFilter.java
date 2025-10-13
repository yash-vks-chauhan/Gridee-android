package com.parking.app.config;

import com.parking.app.model.Users;
import com.parking.app.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        if (requestURI.equals("/api/auth/login") ||
                requestURI.equals("/api/auth/register") ||
                requestURI.equals("/api/parking-lots/list/by-names") ||
                requestURI.equals("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                String userId = jwtUtil.extractUserId(token);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<Users> userOptional = userRepository.findById(userId);

                    if (userOptional.isPresent()) {
                        Users user = userOptional.get();

                        if (jwtUtil.validateToken(token, userId)) {
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(
                                            user,
                                            null,
                                            Collections.emptyList()
                                    );

                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    }
                }
            } catch (Exception e) {
                // Optionally log exception
            }
        }

        filterChain.doFilter(request, response);
    }
}
