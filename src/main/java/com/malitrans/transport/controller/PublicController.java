package com.malitrans.transport.controller;

import com.malitrans.transport.model.DeliveryCompany;
import com.malitrans.transport.repository.DeliveryCompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/public")
public class PublicController {

    @Autowired
    private DeliveryCompanyRepository repository;

    @GetMapping("/companies")
    public ResponseEntity<?> getActiveCompanies() {
        List<DeliveryCompany> activeCompanies = repository.findByIsActiveTrue();
        
        // Convert to List<Map<String, Object>> with only ID and Name
        List<Map<String, Object>> result = activeCompanies.stream()
                .map(company -> {
                    Map<String, Object> companyMap = new HashMap<>();
                    companyMap.put("id", company.getId());
                    companyMap.put("name", company.getName());
                    return companyMap;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
}

