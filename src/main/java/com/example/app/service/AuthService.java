package com.example.app.service;

import com.example.app.dto.AuthRequest;
import com.example.app.dto.AuthResponse;

public interface AuthService {
    AuthResponse register(AuthRequest request);

    AuthResponse login(AuthRequest request);
}
