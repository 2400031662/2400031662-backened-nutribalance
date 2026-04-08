package com.example.app.dto;

import com.example.app.model.UserRole;

public class AuthResponse {

    private final Long id;
    private final String username;
    private final UserRole role;
    private final String displayName;
    private final String message;

    public AuthResponse(Long id, String username, UserRole role, String displayName, String message) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.displayName = displayName;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMessage() {
        return message;
    }
}
