package com.Shubham.carDealership.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private boolean success;
    private String message;
    private UserDto user;
    private String sessionToken;

    public static AuthResponse success(UserDto user, String token) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setUser(user);
        response.setSessionToken(token);
        return response;
    }

    public static AuthResponse error(String message) {
        AuthResponse response = new AuthResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
