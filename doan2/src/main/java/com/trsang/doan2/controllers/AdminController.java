package com.trsang.doan2.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.trsang.doan2.dtos.admin.UserAdminResponse;
import com.trsang.doan2.dtos.admin.UserStatusUpdateRequest;
import com.trsang.doan2.dtos.requests.RevokeTokenRequest;
import com.trsang.doan2.dtos.responses.MessageResponse;
import com.trsang.doan2.entities.RefreshToken;
import com.trsang.doan2.events.AuthenticationEvent;
import com.trsang.doan2.repositories.IRefreshTokenRepository;
import com.trsang.doan2.repositories.IUserRepository;
import com.trsang.doan2.services.interfaces.IAdminUserService;
import com.trsang.doan2.services.interfaces.IAuthService;
import com.trsang.doan2.services.interfaces.IRefreshTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Admin" , description = "Admin APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
    
    private final IAuthService authService;
    private final IRefreshTokenService refreshTokenService;
    private final IUserRepository userRepository;
    private final IRefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final IAdminUserService adminUserService;

    @PostMapping("/tokens/revoke")
    @Operation(
            summary = "Revoke refresh token (Admin)",
            description = "Admin endpoint to invalidate any refresh token",
            responses = {
                @ApiResponse(
                            responseCode = "200",
                            description = "Refresh tokens revoked successfully",
                            content = @Content(schema = @Schema(implementation = MessageResponse.class))),
                @ApiResponse(
                            responseCode = "400",
                            description = "Invalid token or token not found"),
                @ApiResponse(
                            responseCode = "403", 
                            description = "Access denied"),
        }
    )
    public ResponseEntity<MessageResponse> revokeToken(@Valid @RequestBody RevokeTokenRequest request, HttpServletRequest httpRequest) {
        String adminReason = "Admin revocation: " + (request.getReason() != null ? request.getReason() : "No reason provided");
        MessageResponse response = authService.revokeToken(request.getToken(), adminReason);

        if (response.isSuccess()) {
            eventPublisher.publishEvent(new AuthenticationEvent(
                    this,
                    "admin-action",
                    AuthenticationEvent.AuthEventType.INVALID_TOKEN,
                    "Admin revoked token: " + adminReason,
                    getClientIp(httpRequest)
            ));
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tokens")
    @Operation(
            summary = "List all tokens",
            description = "Lis all refersh tokens in the system (Admin)",
            responses = {
                @ApiResponse(
                            responseCode = "200",
                            description = "List of tokens"
                ),

                @ApiResponse(
                            responseCode = "403", 
                            description = "Access denied"
                )
        }
    )
    public ResponseEntity<Map<String, Object>> listAllTokens() {

        List<RefreshToken> tokens = refreshTokenRepository.findAll();

        List <Map<String, Object>> tokeList = tokens.stream()
                .map(token -> {
                    Map<String, Object> tokenInfo = new HashMap<>();
                    tokenInfo.put("id", token.getId().toString());
                    tokenInfo.put("username", token.getUser().getUsername());
                    tokenInfo.put("active", token.isActive());
                    tokenInfo.put("used", token.isUsed());
                    tokenInfo.put("revoked", token.isRevoked());
                    tokenInfo.put("expiryDate", token.getExpiryDate());
                    tokenInfo.put("createdAt", token.getCreatedAt());
                    tokenInfo.put("reasonRevoked", token.getReasonRevoked());
                    return tokenInfo;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("tokens", tokeList);
        response.put("count", tokens.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/tokens/purge")
    @Operation(
            summary = "Purge expired tokens",
            description = "Purge expired tokens from the system",
            responses = {
                @ApiResponse(
                            responseCode = "200",
                            description = "Tokens purged successfully"),
                @ApiResponse(
                            responseCode = "403", 
                            description = "Access denied"),
        }
    )
    public ResponseEntity<MessageResponse> purgeExpiredTokens() {
        refreshTokenService.purgeExpiredTokens();

        return ResponseEntity.ok(MessageResponse.builder()
                .message("Expired tokens purged successfully")
                .success(true)
                .build());
    }
    
    @PostMapping("/users/{username}/revoke-tokens")
    @Operation(
            summary = "Revoke all tokens for a user",
            description = "Revoke all refresh tokens for a specific user",
            responses = {
                @ApiResponse(
                            responseCode = "200",
                            description = "Tokens revoked successfully"),
                @ApiResponse(
                            responseCode = "400",
                            description = "Invalid username or user not found"),
                @ApiResponse(
                            responseCode = "403", 
                            description = "Access denied"),
        }
    )
    public ResponseEntity<MessageResponse> revokeUserTokens(
                @PathVariable String username,
                @RequestParam(required = false) String reason,
                HttpServletRequest httpRequest) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    String finalReason = "Admin revocation: " + (reason != null ? reason : "Security policy");
                    refreshTokenService.deleteByUser(user);

                    eventPublisher.publishEvent(new AuthenticationEvent(
                            this,
                            "admin-action",
                            AuthenticationEvent.AuthEventType.INVALID_TOKEN,
                            "Admin revoked all tokens for user: " + username + " - Reason: " + finalReason,
                            getClientIp(httpRequest)
                    ));

                    return ResponseEntity.ok(MessageResponse.builder()
                            .message("All tokens for user " + username + " revoked successfully")
                            .success(true)
                            .build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/users")
    @Operation(summary = "List all users with pagination and filtering")
    public ResponseEntity<Page<UserAdminResponse>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        
        try {
            Page<UserAdminResponse> users = adminUserService.findAllUsers(search, isActive, pageable);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user detail by ID")
    public ResponseEntity<UserAdminResponse> getUserDetails(@PathVariable UUID userId) {
        try {
            UserAdminResponse user = adminUserService.getUserDetails(userId);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error fetching user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Update user status")
    public ResponseEntity<MessageResponse> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UserStatusUpdateRequest request) {

        try {
            MessageResponse response = adminUserService.updateUserStatus(userId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user status: {}", userId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse (e.getMessage(), false));

        }     
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete a user")
    public ResponseEntity<MessageResponse> deleteUser(@PathVariable UUID userId) {
        try {
            MessageResponse response = adminUserService.deleteUser(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting user: {}", userId, e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }

    private String getClientIp(HttpServletRequest httpRequest) {
        String xfHeader = httpRequest.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return httpRequest.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}

