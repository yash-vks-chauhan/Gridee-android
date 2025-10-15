package com.parking.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Custom CSRF Token Request Handler that bypasses CSRF validation for JWT-authenticated requests.
 *
 * If the request contains an Authorization header with a Bearer token (JWT),
 * CSRF validation is skipped since JWT provides sufficient authentication.
 *
 * For cookie-based authentication (without JWT), CSRF validation is enforced.
 */
public class JwtCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestAttributeHandler delegate = new CsrfTokenRequestAttributeHandler();

    public JwtCsrfTokenRequestHandler() {
        // Set CSRF request attribute name for proper token handling
        delegate.setCsrfRequestAttributeName("_csrf");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        // Delegate to default handler for token creation and storage
        delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        // Check if request contains Authorization header with Bearer token
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            // Request has JWT token - bypass CSRF validation
            // Return the expected token value to make validation pass
            return csrfToken.getToken();
        }

        // No JWT token - perform normal CSRF validation
        // This will check if X-XSRF-TOKEN header matches the cookie value
        return delegate.resolveCsrfTokenValue(request, csrfToken);
    }
}

