package com.trsang.doan2.services.implementation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.trsang.doan2.dtos.facebook.FacebookDebugTokenResponse;
import com.trsang.doan2.dtos.facebook.FacebookUserResponse;
import com.trsang.doan2.dtos.requests.LoginRequest;
import com.trsang.doan2.dtos.requests.LogoutRequest;
import com.trsang.doan2.dtos.requests.RefreshTokenRequest;
import com.trsang.doan2.dtos.requests.RegisterRequest;
import com.trsang.doan2.dtos.requests.UserOauthRequest;
import com.trsang.doan2.dtos.responses.JwtResponse;
import com.trsang.doan2.dtos.responses.MessageResponse;
import com.trsang.doan2.entities.RefreshToken;
import com.trsang.doan2.entities.Role;
import com.trsang.doan2.entities.User;
import com.trsang.doan2.events.AuthProvider;
import com.trsang.doan2.exceptions.AccountDeactivatedException;
import com.trsang.doan2.exceptions.RefreshTokenException;
import com.trsang.doan2.repositories.IRoleRepository;
import com.trsang.doan2.repositories.IUserRepository;
import com.trsang.doan2.security.UserDetailsImpl;
import com.trsang.doan2.services.interfaces.IAuthService;
import com.trsang.doan2.services.interfaces.IRefreshTokenService;
import com.trsang.doan2.services.interfaces.ITokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import com.trsang.doan2.services.interfaces.IOtpService;
import com.trsang.doan2.services.interfaces.IEmailService;
import com.trsang.doan2.entities.Otp;

@Slf4j
@Service
// @RequiredArgsConstructor
public class AuthService implements IAuthService {
    private final IOtpService otpService;
    private final IEmailService emailService;
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final ITokenService tokenService;
    private final IRefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private final GoogleIdTokenVerifier googleTokenVerifier;
    private final WebClient webClient;
    private final String facebookClientId;
    private final String facebookClientSecret;

    public AuthService(
            IOtpService otpService,
            IEmailService emailService,
            IUserRepository userRepository,
            IRoleRepository roleRepository,
            ITokenService tokenService,
            IRefreshTokenService refreshTokenService,
            PasswordEncoder passwordEncoder,
            ObjectProvider<AuthenticationManager> authenticationManagerProvider,
            WebClient.Builder webClientBuilder,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId,
            @Value("${spring.security.oauth2.client.registration.facebook.client-id}") String facebookClientId,
            @Value("${spring.security.oauth2.client.registration.facebook.client-secret}") String facebookClientSecret) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManagerProvider = authenticationManagerProvider;
        this.googleTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(List.of("533216430410-3f8da959gfvfjd3hhsvboot644gb1smg.apps.googleusercontent.com"))
                .build();
        this.webClient = webClientBuilder.baseUrl("https://graph.facebook.com").build();
        this.facebookClientId = facebookClientId;
        this.facebookClientSecret = facebookClientSecret;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        try {
            // Authenticate using injected AuthenticationManager
            AuthenticationManager authenticationManager = authenticationManagerProvider.getObject();

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
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "User not found with username: " + userDetails.getUsername()));

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
                    .userCode(userDetails.getUserCode())
                    .roles(roles)
                    .build();
        } catch (Exception e) {
            log.error("Authentication error: ", e);
            throw e;
        }
    }

    @Override
    public JwtResponse authenticateUser(UserOauthRequest loginRequest) {
        User user = switch (loginRequest.getProvider()) {
            case GOOGLE -> {
                log.info("Starting Google OAuth authentication for token: {}", loginRequest.getToken());
                Payload payload = verifyGoogleToken(loginRequest.getToken());
                if (payload == null) {
                    log.warn("Invalid Google Token received");
                    throw new BadCredentialsException("Invalid Google Token");
                }
                String email = payload.getEmail();
                log.info("Google OAuth payload email: {}", email);
                yield userRepository.findByEmail(email)
                        .orElseGet(() -> createNewGoogleUser(payload));
            }

            case FACEBOOK -> {
                log.info("Starting Facebook OAuth authentication for token: {}", loginRequest.getToken());
                FacebookUserResponse facebookUser = verifyFacebookToken(loginRequest.getToken());
                if (facebookUser == null) {
                    log.warn("Invalid Facebook Access Token received");
                    throw new BadCredentialsException("Invalid Facebook Access Token");
                }
                String email = facebookUser.getEmail();
                log.info("Facebook OAuth payload email: {}", email);
                yield userRepository.findByEmail(email)
                        .orElseGet(() -> createNewFacebookUser(facebookUser));
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported authentication provider: " + loginRequest.getProvider());
        };
        log.info("User authenticated: {} (ID: {})", user.getUsername(), user.getId());

        if (!user.isActive()) {
            log.warn("User account is deactivated: {}", user.getUsername());
            throw new AccountDeactivatedException("User account is deactivated");
        }

        // Manually create authentication for the SecurityContext
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("Generating JWT response for user: {}", user.getUsername());
        return generateJwtResponse(userDetails);
    }

    private JwtResponse generateJwtResponse(UserDetailsImpl userDetails) {
        String accessToken = tokenService.generateAccessToken(userDetails);
        RefreshToken refreshToken;
        try {
            refreshToken = refreshTokenService
                    .createRefreshToken(userRepository.findByUsername(userDetails.getUsername()).orElseThrow());
        } catch (DataIntegrityViolationException ex) {
            log.warn("Constraint violation creating refresh token, checking for existing tokens");
            List<RefreshToken> activeTokens = refreshTokenService
                    .findActiveTokensByUser(userRepository.findByUsername(userDetails.getUsername()).orElseThrow());
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
                .userCode(userDetails.getUserCode())
                .roles(roles)
                .build();
    }

    private Payload verifyGoogleToken(String token) {
        if (token != null && token.startsWith("ya29")) {
            // It's an access token, use Google's UserInfo endpoint
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<Map> response = restTemplate.exchange("https://www.googleapis.com/oauth2/v3/userinfo",
                        HttpMethod.GET, entity, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> map = response.getBody();
                    Payload payload = new Payload();
                    payload.setEmail((String) map.get("email"));
                    payload.set("given_name", map.get("given_name"));
                    payload.set("family_name", map.get("family_name"));
                    payload.set("picture", map.get("picture"));
                    return payload;
                }
            } catch (Exception e) {
                log.error("Error verifying Google Access Token", e);
            }
            return null;
        } else {
            // It's an ID token, verify using GoogleIdTokenVerifier
            try {
                GoogleIdToken idToken = googleTokenVerifier.verify(token);
                return idToken != null ? idToken.getPayload() : null;
            } catch (Exception e) {
                log.error("Error verifying Google ID token", e);
                return null;
            }
        }
    }

    private FacebookUserResponse verifyFacebookToken(String token) {
        // Step 1: Verify the token is valid and belongs to our app
        String appAccessToken = facebookClientId + "|" + facebookClientSecret;
        FacebookDebugTokenResponse debugResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/debug_token")
                        .queryParam("input_token", token)
                        .queryParam("access_token", appAccessToken)
                        .build())
                .retrieve()
                .bodyToMono(FacebookDebugTokenResponse.class)
                .doOnError(e -> log.error("Error debugging Facebook token", e))
                .onErrorResume(e -> Mono.empty()) // Handle errors by returning an empty Mono
                .block();

        if (debugResponse == null || !debugResponse.getData().isValid()
                || !debugResponse.getData().getAppId().equals(facebookClientId)) {
            log.warn("Invalid Facebook token or token does not belong to our app. App ID from token: {}",
                    debugResponse != null && debugResponse.getData() != null ? debugResponse.getData().getAppId()
                            : "N/A");
            return null;
        }

        // Step 2: Fetch the user's profile
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/me")
                        .queryParam("fields", "id,email,first_name,last_name,picture.type(large)")
                        .queryParam("access_token", token)
                        .build())
                .retrieve()
                .bodyToMono(FacebookUserResponse.class)
                .doOnError(e -> log.error("Error fetching user profile from Facebook", e))
                .onErrorResume(e -> Mono.empty()) // Handle errors by returning an empty Mono
                .block();
    }

    private User createNewGoogleUser(Payload payload) {
        String email = payload.getEmail();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String avatarUrl = (String) payload.get("picture");

        // Create a unique username as it's a required field. Use email by default.
        String username = email;
        if (userRepository.existsByUsername(username)) {
            // If email-based username exists, append a short UUID to make it unique
            username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Default role 'ROLE_USER' not found."));

        User newUser = User.builder()
                .email(email)
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .avatarUrl(avatarUrl)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Set a random, unusable password
                .provider(AuthProvider.GOOGLE)
                .isActive(true)
                .roles(Collections.singleton(userRole))
                .build();

        log.info("Creating new user from Google OAuth: {}", email);
        return userRepository.save(newUser);
    }

    private User createNewFacebookUser(FacebookUserResponse facebookUser) {
        String email = facebookUser.getEmail();
        if (email == null || email.isBlank()) {
            // Handle case where Facebook user does not provide an email
            // You might want to throw an exception or create a user without an email
            // For now, we'll use the facebook ID to create a placeholder email
            email = facebookUser.getId() + "@facebook.com";
        }

        // Create a unique username
        String username = email;
        if (userRepository.existsByUsername(username)) {
            username = username + "_" + UUID.randomUUID().toString().substring(0, 4);
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Default role 'ROLE_USER' not found."));

        String avatarUrl = (facebookUser.getPicture() != null && facebookUser.getPicture().getData() != null)
                ? facebookUser.getPicture().getData().getUrl()
                : null;

        User newUser = User.builder()
                .email(email)
                .username(username)
                .firstName(facebookUser.getFirstName())
                .lastName(facebookUser.getLastName())
                .avatarUrl(avatarUrl)
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // Random password
                .provider(AuthProvider.FACEBOOK)
                .isActive(true)
                .roles(Collections.singleton(userRole))
                .build();

        log.info("Creating new user from Facebook OAuth: {}", email);
        return userRepository.save(newUser);
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
                            .userCode(userDetails.getUserCode())
                            .roles(roles)
                            .build();
                })
                .orElseThrow(
                        () -> new RefreshTokenException(requestRefreshToken, "Refresh token not found in database"));
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

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Default role 'ROLE_USER' not found."));
        roles.add(userRole);
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

    @Override
    public MessageResponse forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return MessageResponse.builder().message("User with this email not found").success(false).build();
        }

        Otp otp = otpService.generateOtp(user);
        emailService.sendOtpEmail(user.getEmail(), otp.getOtpCode());

        return MessageResponse.builder().message("OTP has been sent to your email").success(true).build();
    }

    @Override
    public MessageResponse verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return MessageResponse.builder().message("User not found").success(false).build();
        }

        String resetToken = otpService.verifyOtp(user, otpCode);
        if (resetToken != null) {
            return MessageResponse.builder().message("OTP verified successfully").success(true).token(resetToken)
                    .build();
        } else {
            return MessageResponse.builder().message("Invalid or expired OTP").success(false).build();
        }
    }

    @Override
    public MessageResponse resetPassword(String email, String resetToken, String newPassword) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return MessageResponse.builder().message("User not found").success(false).build();
        }

        if (!otpService.validateResetToken(user, resetToken)) {
            return MessageResponse.builder().message("Invalid reset token").success(false).build();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Optionally, we could invalidate the resetToken here by deleting the OTP
        // record or clearing the token.
        // For simplicity, we just save the new password.

        return MessageResponse.builder().message("Password reset successfully").success(true).build();
    }
}
