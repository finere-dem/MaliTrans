package com.malitrans.transport.controller;

import com.malitrans.transport.dto.DriverDossierDTO;
import com.malitrans.transport.dto.GuarantorDTO;
import com.malitrans.transport.dto.IdentityDocumentRequest;
import com.malitrans.transport.security.SecurityUtil;
import com.malitrans.transport.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/driver")
@PreAuthorize("hasAuthority('CHAUFFEUR')")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @Operation(summary = "Ajouter un garant", 
               description = "Ajoute un garant au chauffeur authentifié. " +
                           "Un chauffeur doit avoir exactement 2 garants. " +
                           "L'ID du chauffeur est automatiquement extrait du JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Garant ajouté avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides ou limite de garants atteinte"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @PostMapping("/guarantors")
    public ResponseEntity<GuarantorDTO> addGuarantor(@RequestBody GuarantorDTO guarantorDTO) {
        try {
            // SECURITY: Extract driver ID from JWT (Zero Trust)
            Long driverId = SecurityUtil.getCurrentUserId();
            
            GuarantorDTO result = driverService.addGuarantor(driverId, guarantorDTO);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @Operation(summary = "Lister les garants", 
               description = "Retourne la liste des garants du chauffeur authentifié.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des garants"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @GetMapping("/guarantors")
    public ResponseEntity<List<GuarantorDTO>> getGuarantors() {
        // SECURITY: Extract driver ID from JWT (Zero Trust)
        Long driverId = SecurityUtil.getCurrentUserId();
        
        List<GuarantorDTO> guarantors = driverService.getGuarantors(driverId);
        return ResponseEntity.ok(guarantors);
    }

    @Operation(summary = "Demander l'activation", 
               description = "Demande l'activation du compte chauffeur. " +
                           "Vérifie que le chauffeur a uploadé son document d'identité et a 2 garants. " +
                           "Change le statut à PENDING_VALIDATION pour validation par l'admin.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Demande d'activation soumise avec succès"),
        @ApiResponse(responseCode = "400", description = "Prérequis non remplis (document manquant, garants insuffisants)"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @PostMapping("/request-activation")
    public ResponseEntity<Map<String, String>> requestActivation() {
        try {
            // SECURITY: Extract driver ID from JWT (Zero Trust)
            Long driverId = SecurityUtil.getCurrentUserId();
            
            driverService.requestActivation(driverId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Activation request submitted successfully. Waiting for admin validation.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Update identity document URL", 
               description = "Update the identity document URL for the authenticated driver. " +
                           "The URL should be obtained from the file upload endpoint.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Identity document URL updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request (empty URL or invalid driver)"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @PutMapping("/me/identity-document")
    public ResponseEntity<Map<String, String>> updateIdentityDocument(
            @RequestBody IdentityDocumentRequest request) {
        try {
            // SECURITY: Extract driver ID from JWT (Zero Trust)
            Long driverId = SecurityUtil.getCurrentUserId();
            
            driverService.updateIdentityDocument(driverId, request.getIdentityDocumentUrl());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Identity document URL updated successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Get my dossier", 
               description = "Get the authenticated driver's own dossier including identity document, guarantors, status, and profile information.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dossier retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Driver not found"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @GetMapping("/me/dossier")
    public ResponseEntity<DriverDossierDTO> getMyDossier() {
        try {
            // SECURITY: Extract driver ID from JWT (Zero Trust)
            Long driverId = SecurityUtil.getCurrentUserId();
            
            DriverDossierDTO dossier = driverService.getMyDossier(driverId);
            return ResponseEntity.ok(dossier);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

