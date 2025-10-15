package com.parking.app.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/oauth2")
public class OAuth2Controller {

    @GetMapping("/user")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();

        if (authentication == null) {
            result.put("authenticated", false);
            result.put("message", "No authentication present");
            return result;
        }

        result.put("authenticated", authentication.isAuthenticated());
        result.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        if (authentication instanceof OAuth2AuthenticationToken oauth2) {
            result.put("authType", "OAUTH2");
            result.put("name", oauth2.getName());
            result.put("attributes", oauth2.getPrincipal().getAttributes());
        } else {
            // Basic or other auth types
            result.put("authType", authentication.getClass().getSimpleName());
            result.put("name", authentication.getName());
        }

        return result;
    }
}
