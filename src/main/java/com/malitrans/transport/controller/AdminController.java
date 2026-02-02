package com.malitrans.transport.controller;

import com.malitrans.transport.dto.DriverValidationDTO;
import com.malitrans.transport.service.AdminService;
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
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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

