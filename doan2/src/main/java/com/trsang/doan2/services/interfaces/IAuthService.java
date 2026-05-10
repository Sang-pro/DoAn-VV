package com.trsang.doan2.services.interfaces;

import com.trsang.doan2.dtos.requests.LoginRequest;
import com.trsang.doan2.dtos.requests.LogoutRequest;
import com.trsang.doan2.dtos.requests.RefreshTokenRequest;
import com.trsang.doan2.dtos.requests.RegisterRequest;
import com.trsang.doan2.dtos.requests.UserOauthRequest;
import com.trsang.doan2.dtos.responses.JwtResponse;
import com.trsang.doan2.dtos.responses.MessageResponse;

public interface IAuthService {
    JwtResponse authenticateUser(LoginRequest loginRequest);
    JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
    JwtResponse authenticateUser(UserOauthRequest loginRequest);

    MessageResponse registerUser(RegisterRequest registerRequest);
    MessageResponse logoutUser(LogoutRequest logoutRequest);
    MessageResponse revokeToken(String token, String reason);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    MessageResponse forgotPassword(String email);
    MessageResponse verifyOtp(String email, String otp);
    MessageResponse resetPassword(String email, String resetToken, String newPassword);
}
