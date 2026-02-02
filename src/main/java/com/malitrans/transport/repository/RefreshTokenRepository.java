package com.malitrans.transport.repository;

import com.malitrans.transport.model.RefreshToken;
import com.malitrans.transport.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(Utilisateur user);
    void deleteByToken(String token);
}

