package com.malitrans.transport.service;

import com.malitrans.transport.model.DeliveryCompany;
import com.malitrans.transport.repository.DeliveryCompanyRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DeliveryCompanyService {

    private final DeliveryCompanyRepository companyRepository;

    public DeliveryCompanyService(DeliveryCompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Optional<DeliveryCompany> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return companyRepository.findById(id);
    }

    public Optional<DeliveryCompany> findByIdAndActive(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return companyRepository.findByIdAndIsActiveTrue(id);
    }

    public List<DeliveryCompany> findAllActive() {
        return companyRepository.findByIsActiveTrue();
    }
}

