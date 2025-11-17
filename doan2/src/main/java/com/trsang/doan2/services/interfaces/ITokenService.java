package com.trsang.doan2.services.interfaces;

import org.springframework.security.core.Authentication;

import com.trsang.doan2.security.UserDetailsImpl;

public interface ITokenService {
    String generateAccessToken(UserDetailsImpl userPrincipal);
    String generateRefreshToken(UserDetailsImpl userPrincipal);
    String getUsernameFromToken(String token);
    boolean validateToken(String token);
    Authentication getAuthentication(String token);
}
