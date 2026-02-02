package com.malitrans.transport.service;

import com.malitrans.transport.dto.AuthResponse;
import com.malitrans.transport.dto.RefreshTokenRequest;
import com.malitrans.transport.dto.RegisterDTO;

public interface AuthService {
    void register(RegisterDTO registerDTO);
    AuthResponse verifyRegistration(String phone, String code);
    AuthResponse login(String username, String password);
    AuthResponse refreshToken(RefreshTokenRequest request);
}

