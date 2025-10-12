package com.parking.app.controller;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/oauth2")
public class OAuth2Controller {

    @GetMapping("/user")
    public Map<String, Object> getCurrentOAuth2User(OAuth2AuthenticationToken authentication) {
        return authentication.getPrincipal().getAttributes();
    }
}
