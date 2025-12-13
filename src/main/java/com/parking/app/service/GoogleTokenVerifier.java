package com.parking.app.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleTokenVerifier {

    private static final Logger logger = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    private static final List<String> TRUSTED_ISSUERS = List.of("accounts.google.com", "https://accounts.google.com");

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    void initVerifier() {
        if (!StringUtils.hasText(googleClientId)) {
            logger.error("Google client ID is not configured; social sign-in will be disabled");
            return;
        }
        verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    /**
     * Verify the Google ID token against the configured client ID.
     */
    public GoogleIdToken.Payload verify(String idTokenString) {
        if (!StringUtils.hasText(idTokenString)) {
            logger.warn("Google ID token is missing");
            return null;
        }
        if (verifier == null) {
            logger.error("Google ID token verifier is not initialized due to missing client ID configuration");
            return null;
        }
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                logger.warn("Google ID token verification failed (null)");
                return null;
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!TRUSTED_ISSUERS.contains(payload.getIssuer())) {
                logger.warn("Google ID token issuer mismatch. Got {}", payload.getIssuer());
                return null;
            }
            Object audience = payload.getAudience();
            if (audience == null || !googleClientId.equals(audience.toString())) {
                logger.warn("Google ID token audience mismatch. Expected {}, got {}", googleClientId, audience);
                return null;
            }
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                logger.warn("Google account email is not verified");
                return null;
            }
            return payload;
        } catch (GeneralSecurityException | IOException e) {
            logger.error("Error verifying Google ID token: {}", e.getMessage());
            return null;
        }
    }
}
