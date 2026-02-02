package com.malitrans.transport.dto;

public class AuthResponse {
    private String token; // Access token (JWT)
    private String refreshToken; // Refresh token (UUID)
    private String username;
    private String role; // "CLIENT", "CHAUFFEUR", or "ADMIN"
    private Long userId; // CRITICAL: Mobile app needs this to identify the user and navigate to correct home screen

    public AuthResponse() {
    }

    public AuthResponse(String token, String refreshToken, String username, String role, Long userId) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.username = username;
        this.role = role;
        this.userId = userId;
    }

    // Legacy constructor for backward compatibility
    public AuthResponse(String token, String username, String role, Long userId) {
        this.token = token;
        this.refreshToken = null;
        this.username = username;
        this.role = role;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

