package com.malitrans.transport.controller;

import com.malitrans.transport.dto.DriverValidationDTO;
import com.malitrans.transport.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "Vue d'ensemble globale du systÃ¨me",
               description = "Retourne les compteurs globaux utilisateurs, sociÃ©tÃ©s, chauffeurs et courses.")
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(adminService.getOverview());
    }

    @Operation(summary = "Lister les utilisateurs du systÃ¨me",
               description = "Vue globale admin avec filtres optionnels par rÃ´le, statut et recherche.")
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "100") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return ResponseEntity.ok(adminService.getUsers(role, status, q, safeLimit));
    }

    @Operation(summary = "Lister toutes les sociÃ©tÃ©s",
               description = "Retourne les sociÃ©tÃ©s avec compteurs chauffeurs globaux.")
    @GetMapping("/companies")
    public ResponseEntity<List<Map<String, Object>>> getCompanies() {
        return ResponseEntity.ok(adminService.getCompanies());
    }

    @Operation(summary = "Creer une societe",
               description = "Permet au super-admin d'ajouter une nouvelle societe de livraison active.")
    @PostMapping("/companies")
    public ResponseEntity<Map<String, Object>> createCompany(@RequestBody Map<String, Object> request) {
        try {
            if (request == null) {
                request = new HashMap<>();
            }
            String name = request.get("name") != null ? request.get("name").toString() : null;
            String address = request.get("address") != null ? request.get("address").toString() : null;
            return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createCompany(name, address));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Lister les courses du systÃ¨me",
               description = "Retourne les courses rÃ©centes avec filtre optionnel par statut.")
    @GetMapping("/rides")
    public ResponseEntity<List<Map<String, Object>>> getRides(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return ResponseEntity.ok(adminService.getRides(status, safeLimit));
    }

    @Operation(summary = "Suspendre un utilisateur",
               description = "Suspend un utilisateur non-admin et désactive son compte.")
    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<Map<String, Object>> suspendUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(adminService.suspendUser(userId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Réactiver un utilisateur",
               description = "Réactive un utilisateur non-admin et remet son statut à ACTIVE.")
    @PostMapping("/users/{userId}/reactivate")
    public ResponseEntity<Map<String, Object>> reactivateUser(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(adminService.reactivateUser(userId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Activer ou désactiver une société",
               description = "Permet au super-admin de contrôler l'accès d'une société de livraison.")
    @PostMapping("/companies/{companyId}/active")
    public ResponseEntity<Map<String, Object>> setCompanyActive(
            @PathVariable Long companyId,
            @RequestParam boolean active) {
        try {
            return ResponseEntity.ok(adminService.setCompanyActive(companyId, active));
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Lister les chauffeurs en attente de validation", 
               description = "Retourne la liste de tous les chauffeurs avec statut PENDING_VALIDATION, " +
                           "incluant leurs garants et documents.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des chauffeurs en attente"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un admin")
    })
    @GetMapping("/drivers/pending")
    public ResponseEntity<List<DriverValidationDTO>> getPendingDrivers() {
        List<DriverValidationDTO> pendingDrivers = adminService.getPendingDrivers();
        return ResponseEntity.ok(pendingDrivers);
    }

    @Operation(summary = "Activer un chauffeur (Super Admin)", 
               description = "Active un chauffeur en attente d'approbation admin. Change son statut à ACTIVE, " +
                           "lui permettant de recevoir des missions. " +
                           "Étape finale du flux d'activation en 3 étapes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chauffeur activé avec succès"),
        @ApiResponse(responseCode = "400", description = "Chauffeur non trouvé ou statut invalide"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un admin")
    })
    @PostMapping("/drivers/{driverId}/activate")
    public ResponseEntity<Map<String, String>> activateDriver(@PathVariable Long driverId) {
        try {
            adminService.activateDriver(driverId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Driver activated successfully. Status changed to ACTIVE.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Valider un chauffeur (Legacy)", 
               description = "Valide un chauffeur en attente. Change son statut à ACTIVE. " +
                           "Note: Utilisez /activate pour le nouveau flux en 3 étapes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chauffeur validé avec succès"),
        @ApiResponse(responseCode = "400", description = "Chauffeur non trouvé ou statut invalide"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un admin")
    })
    @PostMapping("/drivers/{driverId}/validate")
    public ResponseEntity<Map<String, String>> validateDriver(@PathVariable Long driverId) {
        try {
            adminService.validateDriver(driverId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Driver validated successfully. Status changed to ACTIVE.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Rejeter un chauffeur", 
               description = "Rejette un chauffeur en attente. Change son statut à REJECTED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chauffeur rejeté avec succès"),
        @ApiResponse(responseCode = "400", description = "Chauffeur non trouvé ou statut invalide"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un admin")
    })
    @PostMapping("/drivers/{driverId}/reject")
    public ResponseEntity<Map<String, String>> rejectDriver(
            @PathVariable Long driverId,
            @RequestParam(required = false) String reason) {
        try {
            adminService.rejectDriver(driverId, reason);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Driver rejected successfully. Status changed to REJECTED.");
            if (reason != null && !reason.trim().isEmpty()) {
                response.put("reason", reason);
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

