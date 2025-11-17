package com.trsang.doan2.services.interfaces;

import com.trsang.doan2.dtos.requests.LoginRequest;
import com.trsang.doan2.dtos.requests.LogoutRequest;
import com.trsang.doan2.dtos.requests.RefreshTokenRequest;
import com.trsang.doan2.dtos.requests.RegisterRequest;
import com.trsang.doan2.dtos.responses.JwtResponse;
import com.trsang.doan2.dtos.responses.MessageResponse;

public interface IAuthService {
    JwtResponse authenticateUser(LoginRequest loginRequest);
    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);

    MessageResponse registerUser(RegisterRequest registerRequest);
    MessageResponse logoutUser(LogoutRequest logoutRequest);
    MessageResponse revokeToken(String token, String reason);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
