package com.parking.app.dto;

import lombok.Data;

@Data
public class SocialSignInRequest {
    private String idToken;
    private String email;
    private String name;
    private String profilePicture;
    private String provider;
}
