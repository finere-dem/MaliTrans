package com.malitrans.transport.repository;

import com.malitrans.transport.model.DeliveryCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryCompanyRepository extends JpaRepository<DeliveryCompany, Long> {
    Optional<DeliveryCompany> findByIdAndIsActiveTrue(Long id);
    List<DeliveryCompany> findByIsActiveTrue();
}

