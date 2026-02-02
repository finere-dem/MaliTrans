package com.malitrans.transport.controller;

import com.malitrans.transport.dto.DriverDossierDTO;
import com.malitrans.transport.dto.DriverSummaryDTO;
import com.malitrans.transport.dto.GuarantorDTO;
import com.malitrans.transport.dto.PaginatedResponse;
import com.malitrans.transport.dto.ValidationErrorResponse;
import com.malitrans.transport.exception.ValidationException;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.model.UserStatus;
import com.malitrans.transport.security.SecurityUtil;
import com.malitrans.transport.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/company")
@PreAuthorize("hasAnyAuthority('COMPANY_MANAGER', 'SUPPLIER')")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Operation(summary = "Lister la flotte de chauffeurs (paginé)", 
               description = "Retourne la liste paginée de tous les chauffeurs de l'entreprise du manager. " +
                           "Supporte le filtrage par statut et la recherche par username, téléphone ou matricule. " +
                           "managerId est automatiquement extrait du JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste paginée des chauffeurs récupérée avec succès"),
        @ApiResponse(responseCode = "400", description = "Paramètres invalides"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un company manager ou supplier"),
        @ApiResponse(responseCode = "401", description = "Non authentifié")
    })
    @GetMapping("/drivers")
    public ResponseEntity<PaginatedResponse<DriverSummaryDTO>> getCompanyDrivers(
            @Parameter(description = "Numéro de page (défaut: 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Nombre d'éléments par page (défaut: 20, max: 100)", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Filtre optionnel par statut (PENDING_COMPANY_VERIFICATION, PENDING_ADMIN_APPROVAL, ACTIVE, etc.)", required = false)
            @RequestParam(required = false) String status,
            @Parameter(description = "Recherche optionnelle dans username, téléphone ou matricule", required = false)
            @RequestParam(required = false) String q) {
        
        Long managerId = SecurityUtil.getCurrentUserId();
        
        // Parse status if provided - throw exception if invalid (handled by GlobalExceptionHandler)
        UserStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status - throw exception to be handled by GlobalExceptionHandler
                throw new IllegalArgumentException("Invalid status: " + status + 
                    ". Valid values: " + java.util.Arrays.toString(UserStatus.values()));
            }
        }
        
        PaginatedResponse<DriverSummaryDTO> response = companyService.getCompanyDrivers(
                managerId, page, limit, statusEnum, q);
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Lister les chauffeurs en attente de vérification", 
               description = "Retourne la liste des chauffeurs de l'entreprise du manager avec statut PENDING_COMPANY_VERIFICATION.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des chauffeurs en attente"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un company manager")
    })
    @GetMapping("/drivers/pending")
    public ResponseEntity<List<Utilisateur>> getPendingDrivers() {
        Long managerId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(companyService.getPendingDriversForCompany(managerId));
    }

    @Operation(summary = "Valider un chauffeur", 
               description = "Permet au manager d'entreprise d'ajouter des garants/documents et de valider un chauffeur. " +
                           "Change le statut à PENDING_ADMIN_APPROVAL. " +
                           "Le manager ne peut valider que les chauffeurs de son entreprise.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chauffeur validé avec succès"),
        @ApiResponse(responseCode = "400", description = "Chauffeur non trouvé, statut invalide, ou prérequis non remplis"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un company manager ou chauffeur d'une autre entreprise")
    })
    @PostMapping("/drivers/{driverId}/validate")
    public ResponseEntity<?> validateDriver(
            @PathVariable Long driverId,
            @RequestBody(required = false) List<GuarantorDTO> guarantors) {
        try {
            Long managerId = SecurityUtil.getCurrentUserId();
            
            companyService.validateDriver(driverId, managerId, guarantors);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Driver validated successfully. Status changed to PENDING_ADMIN_APPROVAL.");
            return ResponseEntity.ok(response);
        } catch (ValidationException e) {
            ValidationErrorResponse error = new ValidationErrorResponse(e.getErrorCode(), e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (AccessDeniedException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(403).body(error);
        }
    }

    @Operation(summary = "Obtenir le dossier complet d'un chauffeur", 
               description = "Retourne toutes les informations d'un chauffeur: identité, document d'identité, garants, statut, matricule. " +
                           "Seuls les managers/suppliers de la même entreprise peuvent accéder à ce dossier.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Dossier récupéré avec succès"),
        @ApiResponse(responseCode = "400", description = "Chauffeur non trouvé"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un company manager/supplier de la même entreprise")
    })
    @GetMapping("/drivers/{driverId}/dossier")
    public ResponseEntity<DriverDossierDTO> getDriverDossier(@PathVariable Long driverId) {
        try {
            Long requesterId = SecurityUtil.getCurrentUserId();
            DriverDossierDTO dossier = companyService.getDriverDossier(driverId, requesterId);
            return ResponseEntity.ok(dossier);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).build();
        }
    }
}

