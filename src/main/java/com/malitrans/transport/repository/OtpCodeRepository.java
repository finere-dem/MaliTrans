package com.malitrans.transport.repository;

import com.malitrans.transport.model.OtpCode;
import com.malitrans.transport.model.OtpType;
import com.malitrans.transport.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findByCodeAndType(String code, OtpType type);

    void deleteByUser(Utilisateur user);

    void deleteByUserAndType(Utilisateur user, OtpType type);
}
