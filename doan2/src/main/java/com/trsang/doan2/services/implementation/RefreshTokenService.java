package com.trsang.doan2.services.implementation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trsang.doan2.config.JwtConfig;
import com.trsang.doan2.entities.RefreshToken;
import com.trsang.doan2.entities.User;
import com.trsang.doan2.exceptions.RefreshTokenException;
import com.trsang.doan2.repositories.IRefreshTokenRepository;
import com.trsang.doan2.services.interfaces.IRefreshTokenService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RefreshTokenService implements IRefreshTokenService {

    private final IRefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;

    public RefreshTokenService(IRefreshTokenRepository refreshTokenRepository, JwtConfig jwtConfig) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtConfig = jwtConfig;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String newToken = UUID.randomUUID().toString();
        
        try {
            // First, revoke all existing active tokens
            List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndIsRevokedFalseAndIsUsedFalse(user);
            
            if (!activeTokens.isEmpty()) {
                activeTokens.forEach(token -> {
                    token.setRevoked(true);
                    token.setReasonRevoked("Superseded by new token");
                    token.setReplacedByToken(newToken);
                });
                refreshTokenRepository.saveAll(activeTokens);
                log.debug("Revoked {} previous active tokens for user: {}", activeTokens.size(), user.getUsername());
            }
            
            // Now create a new token
            RefreshToken refreshToken = RefreshToken.builder()
                    .user(user)
                    .token(newToken)
                    .expiryDate(Instant.now().plusMillis(jwtConfig.getRefreshExpirationMs()))
                    .isUsed(false)
                    .isRevoked(false)
                    .build();
                    
            RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
            log.info("New refresh token created for user: {}", user.getUsername());
            
            return savedToken;
        } catch (DataIntegrityViolationException ex) {
            log.error("Could not create refresh token for user {}: {}", 
                    user.getUsername(), ex.getMessage());
            
            // Check if we have any existing token that can be used
            List<RefreshToken> existingTokens = refreshTokenRepository.findByUser(user);
            if (!existingTokens.isEmpty()) {
                // Get the newest token (with highest creation timestamp)
                RefreshToken latestToken = existingTokens.stream()
                        .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                        .orElseThrow();
                
                // If it's not active, make it active
                if (!latestToken.isActive()) {
                    latestToken.setUsed(false);
                    latestToken.setRevoked(false);
                    latestToken.setExpiryDate(Instant.now().plusMillis(jwtConfig.getRefreshExpirationMs()));
                    return refreshTokenRepository.save(latestToken);
                }
                
                return latestToken;
            }
            
            throw new RuntimeException("Failed to create refresh token", ex);
        }
    }

    @Override
    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            token.setRevoked(true);
            token.setReasonRevoked("Token expired");
            refreshTokenRepository.save(token);
            throw new RefreshTokenException(token.getToken(), "Refresh token was expired. Please sign in again.");
        }
        
        if (token.isUsed()) {
            throw new RefreshTokenException(token.getToken(), "Refresh token was already used. Please sign in again.");
        }
        
        if (token.isRevoked()) {
            throw new RefreshTokenException(token.getToken(), "Refresh token was revoked. Please sign in again.");
        }
        
        return token;
    }
    
    @Override
    @Transactional
    public RefreshToken useToken(RefreshToken token, String replacedByToken) {
        // Mark token as used
        token.setUsed(true);
        token.setReplacedByToken(replacedByToken);
        return refreshTokenRepository.save(token);
    }
    
    @Override
    @Transactional
    public void revokeToken(RefreshToken token, String reason) {
        token.setRevoked(true);
        token.setReasonRevoked(reason);
        refreshTokenRepository.save(token);
        log.debug("Refresh token revoked for user: {}, reason: {}", 
                token.getUser().getUsername(), reason);
    }

    @Override
    @Transactional
    public void deleteByUser(User user) {
        // Instead of physical deletion, mark all tokens as revoked
        int updated = refreshTokenRepository.revokeAllUserTokens(user);
        log.info("Revoked {} refresh tokens for user: {}", updated, user.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> findActiveTokensByUser(User user) {
        return refreshTokenRepository.findByUserAndIsRevokedFalseAndIsUsedFalse(user).stream()
                .filter(token -> !token.isExpired())
                .toList();
    }
    
    @Override
    @Transactional
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        int deleted = refreshTokenRepository.deleteAllExpiredTokens(now);
        if (deleted > 0) {
            log.info("Purged {} expired refresh tokens", deleted);
        }
    }
}
