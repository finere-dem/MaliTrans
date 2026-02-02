package com.malitrans.transport.service;

import com.malitrans.transport.exception.TokenRefreshException;
import com.malitrans.transport.model.RefreshToken;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refreshExpiration:604800000}") // Default: 7 days in milliseconds
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UtilisateurService utilisateurService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                              UtilisateurService utilisateurService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.utilisateurService = utilisateurService;
    }

    /**
     * Create a new refresh token for a user
     * @param userId The user ID
     * @return The created refresh token
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        Utilisateur user = utilisateurService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Delete any existing refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // Create new refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify if refresh token is valid and not expired
     * @param token The refresh token string
     * @return The refresh token entity if valid
     * @throws TokenRefreshException if token is invalid or expired
     */
    public RefreshToken verifyExpiration(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenRefreshException(token, "Refresh token not found"));

        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException(token, "Refresh token was expired. Please make a new signin request");
        }

        return refreshToken;
    }

    /**
     * Delete refresh token by user ID (for logout)
     * @param userId The user ID
     */
    @Transactional
    public void deleteByUserId(Long userId) {
        Utilisateur user = utilisateurService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Delete refresh token by token string
     * @param token The refresh token string
     */
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}

