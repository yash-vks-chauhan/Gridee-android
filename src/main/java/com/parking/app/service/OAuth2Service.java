package com.parking.app.service;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OAuth2Service {

    public Map<String, Object> getUserInfo(OAuth2AuthenticationToken authentication) {
        OAuth2User oAuth2User = authentication.getPrincipal();
        return oAuth2User.getAttributes();
    }

    // Example: process user info (register or update user)
    public void processOAuth2User(OAuth2AuthenticationToken authentication) {
        Map<String, Object> userInfo = getUserInfo(authentication);
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        // Add your user registration or update logic here
    }
}
