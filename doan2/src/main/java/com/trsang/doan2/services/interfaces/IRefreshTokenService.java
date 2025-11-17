package com.trsang.doan2.services.interfaces;

import java.util.List;
import java.util.Optional;

import com.trsang.doan2.entities.RefreshToken;
import com.trsang.doan2.entities.User;

public interface IRefreshTokenService {
    Optional<RefreshToken> findByToken(String token);
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyExpiration(RefreshToken token);
    RefreshToken useToken(RefreshToken token, String replacedByToken);
    void revokeToken(RefreshToken token, String reason);
    void deleteByUser(User user);
    List<RefreshToken> findActiveTokensByUser(User user);
    void purgeExpiredTokens();
}
