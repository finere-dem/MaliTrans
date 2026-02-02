package com.malitrans.transport.controller;

import com.malitrans.transport.dto.PaginatedResponse;
import com.malitrans.transport.dto.RideRequestDTO;
import com.malitrans.transport.dto.ValidateCodeDTO;
import com.malitrans.transport.model.UserStatus;
import com.malitrans.transport.model.ValidationStatus;
import com.malitrans.transport.security.SecurityUtil;
import com.malitrans.transport.service.RideRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ride")
public class RideRequestController {

    private final RideRequestService service;

    public RideRequestController(RideRequestService service) {
        this.service = service;
    }

    @Operation(summary = "Créer une demande de trajet", 
               description = "Crée une nouvelle demande selon le modèle Client-Supplier-Driver. " +
                           "Le statut initial dépend du flowType (CLIENT_INITIATED → WAITING_SUPPLIER_VALIDATION, " +
                           "SUPPLIER_INITIATED → WAITING_CLIENT_VALIDATION). " +
                           "clientId/supplierId sont automatiquement extraits du JWT selon le rôle de l'utilisateur.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Demande créée")})
    @PostMapping
    public ResponseEntity<RideRequestDTO> create(@RequestBody RideRequestDTO dto) {
        // SECURITY: Extract current user from JWT
        com.malitrans.transport.model.Utilisateur currentUser = SecurityUtil.getCurrentUser();
        Long currentUserId = currentUser.getId();
        com.malitrans.transport.model.Role currentUserRole = currentUser.getRole();
        
        return ResponseEntity.ok(service.createRideRequest(dto, currentUserId, currentUserRole));
    }

    @Operation(summary = "Lister les demandes prêtes pour la collecte", 
               description = "Retourne les demandes avec ValidationStatus READY_FOR_PICKUP. " +
                             "Réservé aux chauffeurs dont le compte est entièrement validé (UserStatus = ACTIVE). " +
                             "Un chauffeur en PENDING_COMPANY_VERIFICATION ou PENDING_ADMIN_APPROVAL ne peut ni voir ni accepter les courses.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des demandes prêtes"),
        @ApiResponse(responseCode = "403", description = "Chauffeur non actif (compte pas encore validé)")
    })
    @PreAuthorize("hasAuthority('CHAUFFEUR')")
    @GetMapping("/ready")
    public ResponseEntity<?> getReadyForPickup() {
        com.malitrans.transport.model.Utilisateur currentUser = SecurityUtil.getCurrentUser();
        if (currentUser.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
            throw new AccessDeniedException("Only drivers can view available rides");
        }
        if (currentUser.getStatus() == null || currentUser.getStatus() != UserStatus.ACTIVE) {
            String status = currentUser.getStatus() != null ? currentUser.getStatus().name() : "NULL";
            return ResponseEntity.status(403).body(
                java.util.Map.of("error", "Driver account must be fully validated (ACTIVE) to view available rides. Current status: " + status));
        }
        return ResponseEntity.ok(service.getReadyForPickupRequests());
    }

    @Operation(summary = "Récupérer un trajet par ID", 
               description = "Retourne les détails d'un trajet spécifique. Accessible aux Clients, Fournisseurs, Chauffeurs et Admins.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Trajet trouvé"),
        @ApiResponse(responseCode = "404", description = "Trajet non trouvé")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Allow any authenticated user to view details for now
    public ResponseEntity<RideRequestDTO> getRideById(@PathVariable Long id) {
        return service.getRideRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Assigner un chauffeur à une demande (First-Come-First-Served)", 
               description = "Assigne le chauffeur authentifié à une demande prête pour la collecte. " +
                           "Le premier chauffeur à accepter obtient la mission. " +
                           "Utilise le contrôle de concurrence pour éviter les conflits. " +
                           "driverId est automatiquement extrait du JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Chauffeur assigné avec succès"),
        @ApiResponse(responseCode = "404", description = "Demande ou chauffeur non trouvé"),
        @ApiResponse(responseCode = "409", description = "Demande déjà assignée à un autre chauffeur"),
        @ApiResponse(responseCode = "400", description = "Demande non prête pour la collecte ou chauffeur non actif"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @PreAuthorize("hasAuthority('CHAUFFEUR')")
    @PostMapping("/{id}/assign")
    public ResponseEntity<?> assignDriver(@PathVariable Long id) {
        try {
            // SECURITY: Extract driver ID from JWT (Zero Trust)
            Long driverId = SecurityUtil.getCurrentUserId();
            
            // Verify user is actually a driver
            com.malitrans.transport.model.Utilisateur currentUser = SecurityUtil.getCurrentUser();
            if (currentUser.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
                throw new AccessDeniedException("Only drivers can assign themselves to ride requests");
            }
            
            return ResponseEntity.ok(service.assignDriver(id, driverId));
        } catch (com.malitrans.transport.exception.RideAlreadyTakenException e) {
            return ResponseEntity.status(409).body(java.util.Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Valider une demande (Client ou Supplier)", 
               description = "Valide une demande en attente. " +
                           "Change le statut à READY_FOR_PICKUP, génère les QR codes, et notifie tous les chauffeurs. " +
                           "Seul le client ou le supplier lié à la demande peut la valider.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Demande validée avec succès"),
        @ApiResponse(responseCode = "404", description = "Demande non trouvée"),
        @ApiResponse(responseCode = "400", description = "Demande ne peut pas être validée dans son état actuel"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - vous n'êtes pas autorisé à valider cette demande")
    })
    @PreAuthorize("hasAnyAuthority('CLIENT', 'SUPPLIER')")
    @PostMapping("/{id}/validate")
    public ResponseEntity<?> validateRequest(@PathVariable Long id) {
        try {
            // SECURITY: Extract user ID from JWT (Zero Trust)
            Long userId = SecurityUtil.getCurrentUserId();
            
            return ResponseEntity.ok(service.validateRequest(id, userId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Valider la collecte (Pickup)", 
               description = "Valide la collecte du colis avec un code. " +
                           "Transition: DRIVER_ACCEPTED → IN_TRANSIT. " +
                           "driverId est automatiquement extrait du JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Collecte validée avec succès"),
        @ApiResponse(responseCode = "404", description = "Demande non trouvée"),
        @ApiResponse(responseCode = "400", description = "Code incorrect, statut invalide ou chauffeur non assigné"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @PreAuthorize("hasAuthority('CHAUFFEUR')")
    @PostMapping("/{id}/pickup")
    public ResponseEntity<?> validatePickup(@PathVariable Long id, @RequestBody ValidateCodeDTO request) {
        try {
            // SECURITY: Extract driver ID from JWT (Zero Trust)
            Long driverId = SecurityUtil.getCurrentUserId();
            
            // Verify user is actually a driver
            com.malitrans.transport.model.Utilisateur currentUser = SecurityUtil.getCurrentUser();
            if (currentUser.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
                throw new AccessDeniedException("Only drivers can validate pickup");
            }
            
            return ResponseEntity.ok(service.validatePickup(id, driverId, request.getCode()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Valider la livraison (Delivery)", 
               description = "Valide la livraison du colis avec un code. " +
                           "Transition: IN_TRANSIT → COMPLETED. " +
                           "driverId est automatiquement extrait du JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Livraison validée avec succès"),
        @ApiResponse(responseCode = "404", description = "Demande non trouvée"),
        @ApiResponse(responseCode = "400", description = "Code incorrect, statut invalide ou chauffeur non assigné"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @PreAuthorize("hasAuthority('CHAUFFEUR')")
    @PostMapping("/{id}/delivery")
    public ResponseEntity<?> validateDelivery(@PathVariable Long id, @RequestBody ValidateCodeDTO request) {
        try {
            // SECURITY: Extract driver ID from JWT (Zero Trust)
            Long driverId = SecurityUtil.getCurrentUserId();
            
            // Verify user is actually a driver
            com.malitrans.transport.model.Utilisateur currentUser = SecurityUtil.getCurrentUser();
            if (currentUser.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
                throw new AccessDeniedException("Only drivers can validate delivery");
            }
            
            return ResponseEntity.ok(service.validateDelivery(id, driverId, request.getCode()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Scanner un QR code (Pickup ou Delivery)", 
               description = "Valide un QR code scanné par le chauffeur. " +
                           "Type PICKUP: change statut à IN_TRANSIT. " +
                           "Type DELIVERY: change statut à COMPLETED. " +
                           "driverId est automatiquement extrait du JWT. " +
                           "Note: Préférez utiliser /pickup et /delivery pour une meilleure gestion des états.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "QR code validé avec succès"),
        @ApiResponse(responseCode = "404", description = "Demande non trouvée"),
        @ApiResponse(responseCode = "400", description = "QR code invalide ou chauffeur non assigné"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @PreAuthorize("hasAuthority('CHAUFFEUR')")
    @PostMapping("/{id}/scan-qr")
    public ResponseEntity<?> scanQrCode(
            @PathVariable Long id,
            @RequestBody com.malitrans.transport.dto.QrScanRequest qrScanRequest) {
        try {
            // SECURITY: Extract driver ID from JWT (Zero Trust)
            Long driverId = SecurityUtil.getCurrentUserId();
            
            // Verify user is actually a driver
            com.malitrans.transport.model.Utilisateur currentUser = SecurityUtil.getCurrentUser();
            if (currentUser.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
                throw new AccessDeniedException("Only drivers can scan QR codes");
            }
            
            return ResponseEntity.ok(service.scanQrCode(id, driverId, 
                    qrScanRequest.getQrCode(), qrScanRequest.getType()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Historique client", 
               description = "Retourne l'historique des demandes pour le client authentifié. " +
                           "clientId est automatiquement extrait du JWT.")
    @ApiResponses({@ApiResponse(responseCode = "200")})
    @PreAuthorize("hasAuthority('CLIENT')")
    @GetMapping("/client/history")
    public List<RideRequestDTO> historyClient() {
        // SECURITY: Extract client ID from JWT (Zero Trust)
        Long clientId = SecurityUtil.getCurrentUserId();
        return service.historyForClient(clientId);
    }

    @Operation(summary = "Historique fournisseur", 
               description = "Retourne l'historique des demandes pour le fournisseur authentifié. " +
                           "supplierId est automatiquement extrait du JWT.")
    @ApiResponses({@ApiResponse(responseCode = "200")})
    @PreAuthorize("hasAuthority('SUPPLIER')")
    @GetMapping("/supplier/history")
    public List<RideRequestDTO> historySupplier() {
        // SECURITY: Extract supplier ID from JWT (Zero Trust)
        Long supplierId = SecurityUtil.getCurrentUserId();
        return service.historyForSupplier(supplierId);
    }

    @Operation(summary = "Courses actives du chauffeur", 
               description = "Retourne toutes les courses actives (en cours) pour le chauffeur authentifié. " +
                           "chauffeurId est automatiquement extrait du JWT. " +
                           "Retourne les courses avec statut DRIVER_ACCEPTED ou IN_TRANSIT. " +
                           "Un chauffeur peut avoir plusieurs courses actives simultanément (batching). " +
                           "Triées par date de création décroissante (plus récentes en premier).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des courses actives (peut être vide)"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @PreAuthorize("hasAuthority('CHAUFFEUR')")
    @GetMapping("/chauffeur/active")
    public ResponseEntity<List<RideRequestDTO>> getActiveRides() {
        // SECURITY: Extract chauffeur ID from JWT (Zero Trust)
        Long chauffeurId = SecurityUtil.getCurrentUserId();
        List<RideRequestDTO> activeRides = service.getActiveRidesForChauffeur(chauffeurId);
        return ResponseEntity.ok(activeRides);
    }

    @Operation(summary = "Historique chauffeur (paginé)", 
               description = "Retourne l'historique paginé des demandes pour le chauffeur authentifié. " +
                           "chauffeurId est automatiquement extrait du JWT. " +
                           "Retourne uniquement les courses avec statut COMPLETED ou CANCELED, " +
                           "triées par date de création décroissante (plus récentes en premier).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historique récupéré avec succès"),
        @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
    })
    @PreAuthorize("hasAuthority('CHAUFFEUR')")
    @GetMapping("/chauffeur/history")
    public ResponseEntity<PaginatedResponse<RideRequestDTO>> historyChauffeur(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        // SECURITY: Extract chauffeur ID from JWT (Zero Trust)
        Long chauffeurId = SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(service.historyForChauffeur(chauffeurId, page, limit));
    }
}
