package com.parking.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.util.function.Supplier;

/**
 * Skips CSRF processing for requests that already present a JWT Authorization header.
 */
public class JwtCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestAttributeHandler delegate = new CsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       Supplier<CsrfToken> csrfTokenSupplier) {
        if (hasJwtAuthHeader(request)) {
            return; // skip binding when JWT is present
        }
        delegate.handle(request, response, csrfTokenSupplier);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        if (hasJwtAuthHeader(request)) {
            return csrfToken != null ? csrfToken.getToken() : null;
        }
        return delegate.resolveCsrfTokenValue(request, csrfToken);
    }

    private boolean hasJwtAuthHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ");
    }
}
