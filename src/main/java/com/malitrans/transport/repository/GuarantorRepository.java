package com.malitrans.transport.repository;

import com.malitrans.transport.model.Guarantor;
import com.malitrans.transport.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuarantorRepository extends JpaRepository<Guarantor, Long> {
    List<Guarantor> findByDriver(Utilisateur driver);
}

