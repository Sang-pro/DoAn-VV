package com.trsang.doan2.controllers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trsang.doan2.dtos.requests.LoginRequest;
import com.trsang.doan2.dtos.requests.LogoutRequest;
import com.trsang.doan2.dtos.requests.RefreshTokenRequest;
import com.trsang.doan2.dtos.requests.RegisterRequest;
import com.trsang.doan2.dtos.requests.RevokeTokenRequest;
import com.trsang.doan2.dtos.responses.JwtResponse;
import com.trsang.doan2.dtos.responses.MessageResponse;
import com.trsang.doan2.events.AuthenticationEvent;
import com.trsang.doan2.exceptions.AccountDeactivatedException;
import com.trsang.doan2.exceptions.RefreshTokenException;
import com.trsang.doan2.services.interfaces.IAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Đồ án 2", description = "Direct API for project")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticate user with username and password, returns JWT token",
            responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Successfully authenticated",
                        content = @Content(schema = @Schema(implementation = JwtResponse.class))
                    ),
                    @ApiResponse(
                        responseCode = "401", 
                        description = "Invalid username or password"
                    )
            }
    )
    public ResponseEntity<JwtResponse> login(
           @Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        try {
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
            eventPublisher.publishEvent(new AuthenticationEvent(
                    this, 
                    loginRequest.getUsername(), 
                    AuthenticationEvent.AuthEventType.LOGIN_SUCCESS,
                    "Login successful",
                    getClientIp(request)
            ));
            return ResponseEntity.ok(jwtResponse);
        } catch (AccountDeactivatedException e) {
            eventPublisher.publishEvent(new AuthenticationEvent(
                    this, 
                    loginRequest.getUsername(), 
                    AuthenticationEvent.AuthEventType.LOGIN_FAILED,
                    "Account deactivated",
                    getClientIp(request)
            ));
            throw e;
        } catch (BadCredentialsException e) {
            eventPublisher.publishEvent(new AuthenticationEvent(
                    this, 
                    loginRequest.getUsername(), 
                    AuthenticationEvent.AuthEventType.LOGIN_FAILED,
                    "Invalid credentials",
                    getClientIp(request)
            ));
            throw e;
        }
    }

    @PostMapping("/register")
    @Operation(
            summary = "Register new user",
            description = "Register a new user with provided details",
            responses = {
                    @ApiResponse(
                        responseCode = "201",
                        description = "User registered successfully",
                        content = @Content(schema = @Schema(implementation = MessageResponse.class))
                    ),
                    @ApiResponse(
                        responseCode = "400", 
                        description = "Invalid data or username/email already in use"
                    )
            }
    )
    public ResponseEntity<MessageResponse> register (
           @Valid @RequestBody RegisterRequest registerRequest, 
                               HttpServletRequest request) {
    
        MessageResponse response = authService.registerUser(registerRequest);
        if (response.isSuccess()) {
            eventPublisher.publishEvent(new AuthenticationEvent(
                    this, 
                    registerRequest.getUsername(), 
                    AuthenticationEvent.AuthEventType.REGISTER_SUCCESS,
                    "Registration successful",
                    getClientIp(request)
            ));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } 
        else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Logout user and invalidate JWT token",
            responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Successfully logged out",
                        content = @Content(schema = @Schema(implementation = MessageResponse.class))
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MessageResponse> logout(
           HttpServletRequest request, @Valid @RequestBody LogoutRequest logoutRequest) {

        MessageResponse response = authService.logoutUser(logoutRequest);
        if (logoutRequest != null && logoutRequest.getUsername() != null) {
            eventPublisher.publishEvent(new AuthenticationEvent(
                    this, 
                    logoutRequest.getUsername(), 
                    AuthenticationEvent.AuthEventType.LOGOUT,
                    "Logout successful",
                    getClientIp(request)
            ));
        }
        return ResponseEntity.ok(response);
    }
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh JWT token",
            description = "Refresh JWT token using refresh token",
            responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Token refreshed successfully",
                        content = @Content(schema = @Schema(implementation = JwtResponse.class))
                    ),
                    @ApiResponse(
                        responseCode = "403", 
                        description = "Invalid refresh token"
                    )
            }
    )
    public ResponseEntity<JwtResponse> refreshToken(
        @Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
            try {
                JwtResponse response = authService.refreshToken(request);
                eventPublisher.publishEvent(new AuthenticationEvent(
                        this, 
                        response.getUsername(), 
                        AuthenticationEvent.AuthEventType.REFRESH_TOKEN,
                        "Token refreshed successfully",
                        getClientIp(httpRequest)
                ));
                return ResponseEntity.ok(response);
            }
            catch (RefreshTokenException e) {
                eventPublisher.publishEvent(new AuthenticationEvent(
                        this, 
                        "unknown", 
                        AuthenticationEvent.AuthEventType.INVALID_TOKEN,
                        "Token refresh failed",
                        getClientIp(httpRequest)
                ));
                throw e;
            }
    }

    @PostMapping("/revoke")
    @Operation(
            summary = "Revoke refresh token",
            description = "Revoke refresh token to prevent further use",
            responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Refresh token revoked successfully",
                        content = @Content(schema = @Schema(implementation = MessageResponse.class))
                    ),
                    @ApiResponse(
                        responseCode = "400", 
                        description = "Invalid token or token not found"
                    )
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<MessageResponse> revokeToken(
            @Valid @RequestBody RevokeTokenRequest request, HttpServletRequest httpRequest) {
            MessageResponse response = authService.revokeToken(request.getToken(), request.getReason());
            if (response.isSuccess()) {
                eventPublisher.publishEvent(new AuthenticationEvent(
                        this, 
                        "token-user", 
                        AuthenticationEvent.AuthEventType.INVALID_TOKEN,
                        "Token revoked: " + (request.getReason() != null ? request.getReason() : "No reason provided"),
                        getClientIp(httpRequest)
                ));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        // Check if the header is present and not empty
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
