package com.malitrans.transport.controller;

import com.malitrans.transport.dto.UtilisateurDTO;
import com.malitrans.transport.mapper.UtilisateurMapper;
import com.malitrans.transport.model.DeliveryCompany;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.service.DeliveryCompanyService;
import com.malitrans.transport.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UtilisateurService utilisateurService;
    private final UtilisateurMapper utilisateurMapper;
    private final DeliveryCompanyService deliveryCompanyService;

    public UserController(UtilisateurService utilisateurService, 
                         UtilisateurMapper utilisateurMapper,
                         DeliveryCompanyService deliveryCompanyService) {
        this.utilisateurService = utilisateurService;
        this.utilisateurMapper = utilisateurMapper;
        this.deliveryCompanyService = deliveryCompanyService;
    }

    @Operation(summary = "Lister tous les fournisseurs", 
               description = "Retourne la liste de tous les utilisateurs avec le rôle SUPPLIER. " +
                           "Permet aux clients de sélectionner un fournisseur lors de la création d'un trajet.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des fournisseurs"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un client")
    })
    @GetMapping("/suppliers")
    @PreAuthorize("hasAuthority('CLIENT')")
    public ResponseEntity<List<UtilisateurDTO>> getSuppliers() {
        List<UtilisateurDTO> suppliers = utilisateurService.findByRole(Role.SUPPLIER)
                .stream()
                .map(utilisateurMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(suppliers);
    }

    @Operation(summary = "Lister toutes les entreprises actives", 
               description = "Retourne la liste de toutes les entreprises de livraison actives. " +
                           "Permet aux chauffeurs de sélectionner une entreprise lors de l'inscription.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des entreprises")
    })
    @GetMapping("/companies")
    public ResponseEntity<List<Map<String, Object>>> getCompanies() {
        List<Map<String, Object>> companies = deliveryCompanyService.findAllActive()
                .stream()
                .map(company -> {
                    Map<String, Object> companyMap = new HashMap<>();
                    companyMap.put("id", company.getId());
                    companyMap.put("name", company.getName());
                    companyMap.put("address", company.getAddress());
                    return companyMap;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(companies);
    }
}

