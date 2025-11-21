package com.trsang.doan2.services.implementation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trsang.doan2.dtos.requests.LoginRequest;
import com.trsang.doan2.dtos.requests.LogoutRequest;
import com.trsang.doan2.dtos.requests.RefreshTokenRequest;
import com.trsang.doan2.dtos.requests.RegisterRequest;
import com.trsang.doan2.dtos.responses.JwtResponse;
import com.trsang.doan2.dtos.responses.MessageResponse;
import com.trsang.doan2.entities.RefreshToken;
import com.trsang.doan2.entities.Role;
import com.trsang.doan2.entities.User;
import com.trsang.doan2.exceptions.RefreshTokenException;
import com.trsang.doan2.repositories.IRoleRepository;
import com.trsang.doan2.repositories.IUserRepository;
import com.trsang.doan2.security.UserDetailsImpl;
import com.trsang.doan2.services.interfaces.IAuthService;
import com.trsang.doan2.services.interfaces.IRefreshTokenService;
import com.trsang.doan2.services.interfaces.ITokenService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthService implements IAuthService {
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final ITokenService tokenService;
    private final IRefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            IUserRepository userRepository, 
            IRoleRepository roleRepository, 
            ITokenService tokenService,
            IRefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        try {
            // Authenticate using injected AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // Generate JWT access token
            String accessToken = tokenService.generateAccessToken(userDetails);
            
            // Get user for refresh token
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + userDetails.getUsername()));
            
            // Create refresh token with retry mechanism
            RefreshToken refreshToken;
            try {
                refreshToken = refreshTokenService.createRefreshToken(user);
            } catch (DataIntegrityViolationException ex) {
                // If we hit a constraint violation, try to find existing token
                log.warn("Constraint violation creating refresh token, checking for existing tokens");
                List<RefreshToken> activeTokens = refreshTokenService.findActiveTokensByUser(user);
                if (activeTokens.isEmpty()) {
                    throw new RuntimeException("Could not create or find valid refresh token");
                }
                refreshToken = activeTokens.get(0);
            }

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            return JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .id(userDetails.getId())
                    .username(userDetails.getUsername())
                    .email(userDetails.getEmail())
                    .roles(roles)
                    .build();
        } catch (Exception e) {
            log.error("Authentication error: ", e);
            throw e;
        }
    }

    @Override
    public JwtResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String requestRefreshToken = refreshTokenRequest.getRefreshToken();
        
        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(refreshToken -> {
                    User user = refreshToken.getUser();
                    UserDetailsImpl userDetails = UserDetailsImpl.build(user);
                    
                    // Generate new access token
                    String accessToken = tokenService.generateAccessToken(userDetails);
                    
                    // Generate new refresh token with retry mechanism
                    RefreshToken newRefreshToken;
                    try {
                        newRefreshToken = refreshTokenService.createRefreshToken(user);
                    } catch (DataIntegrityViolationException ex) {
                        // If we hit a constraint violation, try to find an active token
                        List<RefreshToken> activeTokens = refreshTokenService.findActiveTokensByUser(user);
                        if (activeTokens.isEmpty()) {
                            throw new RefreshTokenException(requestRefreshToken, 
                                "Could not create new refresh token due to constraint violation");
                        }
                        newRefreshToken = activeTokens.get(0);
                    }
                    
                    // Mark old token as used and specify which token replaced it
                    refreshTokenService.useToken(refreshToken, newRefreshToken.getToken());

                    List<String> roles = userDetails.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .toList();
                    
                    return JwtResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .id(userDetails.getId())
                            .username(userDetails.getUsername())
                            .email(userDetails.getEmail())
                            .roles(roles)
                            .build();
                })
                .orElseThrow(() -> new RefreshTokenException(requestRefreshToken, "Refresh token not found in database"));
    }

    @Override
    @Transactional
    public MessageResponse registerUser(RegisterRequest registerRequest) {
        if (existsByEmail(registerRequest.getEmail())) {
            return MessageResponse.builder()
                    .message("Email already in use")
                    .success(false)
                    .build();
        }

        if (existsByUsername(registerRequest.getUsername())) {
            return MessageResponse.builder()
                    .message("Username already in use")
                    .success(false)
                    .build();
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .phoneNumber(registerRequest.getPhoneNumber())
                .build();

        Set<String> strRoles = registerRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null || strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } 
        else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;

                    case "mod":
                        Role modRole = roleRepository.findByName("ROLE_MODERATOR")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                        break;
                
                    default:
                        Role userRole = roleRepository.findByName("ROLE_USER")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        return MessageResponse.builder()
                .message("User registered successfully")
                .success(true)
                .build();
    }

    @Override
    public MessageResponse logoutUser(LogoutRequest logoutRequest) {
        if (logoutRequest != null && logoutRequest.getUsername() != null) {
            userRepository.findByUsername(logoutRequest.getUsername())
                    .ifPresent(refreshTokenService::deleteByUser);
        }
        
        return MessageResponse.builder()
                .message("Logout successful")
                .success(true)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse revokeToken(String token, String reason) {
        return refreshTokenService.findByToken(token).map(refreshToken -> {
            if (refreshToken.isRevoked()) {
                return MessageResponse.builder()
                        .message("Token already revoked")
                        .success(false)
                        .build();
            }

            String finalReason = (reason != null && reason.isBlank()) ? reason : "Manually revoked by user";
            refreshTokenService.revokeToken(refreshToken, finalReason);

            log.info("Token revoked for user: {}, reason: {}", refreshToken.getUser().getUsername(), finalReason);
            return MessageResponse.builder()
                    .message("Token revoked successfully")
                    .success(true)
                    .build();
        })
        .orElse(MessageResponse.builder()
                        .message("Token not found")
                        .success(false)
                        .build());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

}
