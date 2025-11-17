package com.trsang.doan2.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trsang.doan2.dtos.requests.ChangePasswordRequest;
import com.trsang.doan2.dtos.requests.UpdateProfileRequest;
import com.trsang.doan2.dtos.responses.MessageResponse;
import com.trsang.doan2.dtos.responses.UserProfileResponse;
import com.trsang.doan2.exceptions.ServiceException;
import com.trsang.doan2.services.interfaces.IUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {
    
    private final IUserService userService;

    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Get the profile information of the authenticated user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User profile retrieved successfully",
                            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "User not found"
                    )
            }
    )
    public ResponseEntity<?> getCurrentUserProfile(Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Getting profile for authenticated user: {}", username);
            UserProfileResponse profile = userService.getUserProfile(username);
            return ResponseEntity.ok(profile);
        } catch (ServiceException e) {
            log.error("Error retrieving user profile: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(e.getMessage(), false));
        } catch (Exception e) {
            log.error("Unexpected error retrieving user profile", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("An unexpected error occurred", false));
        }
    }

    @PutMapping("/me")
    @Operation(
            summary = "Update user profile",
            description = "Update the profile information of the authenticated user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "User profile updated successfully",
                            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request, invalid input"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    )
            }
    )
    public ResponseEntity<?> updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        try {
            String username = authentication.getName();
            log.info("Updating profile for user: {}", username);
            UserProfileResponse updatedProfile = userService.updateProfile(username, request);
            return ResponseEntity.ok(updatedProfile);
        } catch (ServiceException e) {
            log.error("Error updating user profile: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(e.getMessage(), false));
        } catch (Exception e) {
            log.error("Unexpected error updating user profile", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("An unexpected error occurred", false));
        }
    }

    @PostMapping("/change-password")
    @Operation(
            summary = "Change user password",
            description = "Change the password of the authenticated user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Password changed successfully",
                            content = @Content(schema = @Schema(implementation = MessageResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request, invalid input"
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    )
            }
    )
    public ResponseEntity<?> changePassword(Authentication authentication, 
                                            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            String username = authentication.getName();
            log.info("Changing password for user: {}", username);
            MessageResponse response = userService.changePassword(username, request);
            return ResponseEntity.ok(response);
        } catch (ServiceException e) {
            log.error("Error changing password: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(e.getMessage(), false));
        } catch (Exception e) {
            log.error("Unexpected error changing password", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("An unexpected error occurred", false));
        }
    }

    @PutMapping("/avatar")
    @Operation(
            summary = "Update user avatar",
            description = "Update the avatar URL of the authenticated user",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Avatar updated successfully",
                            content = @Content(schema = @Schema(implementation = MessageResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized"
                    )
            }
    )
    public ResponseEntity<?> updateAvatar(Authentication authentication, @RequestBody String avatarUrl) {
        try {
            String username = authentication.getName();
            log.info("Updating avatar for user: {}", username);
            MessageResponse response = userService.updateAvatar(username, avatarUrl);
            return ResponseEntity.ok(response);
        } catch (ServiceException e) {
            log.error("Error updating avatar: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MessageResponse(e.getMessage(), false));
        } catch (Exception e) {
            log.error("Unexpected error updating avatar", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("An unexpected error occurred", false));
        }
    }    
}
