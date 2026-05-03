package com.malitrans.transport.controller;

import com.malitrans.transport.model.RideRequest;
import com.malitrans.transport.model.ValidationStatus;
import com.malitrans.transport.service.RideRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/public/validate")
@CrossOrigin(origins = "*") // Public recipient validation endpoint.
public class PublicValidationController {

    private final RideRequestService rideRequestService;

    public PublicValidationController(RideRequestService rideRequestService) {
        this.rideRequestService = rideRequestService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getPublicRideInfo(@PathVariable String token) {
        Optional<RideRequest> requestOpt = rideRequestService.getRideRequestByValidationToken(token);
        
        if (requestOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Lien de validation invalide ou expiré."));
        }
        
        RideRequest request = requestOpt.get();
        
        if (request.getValidationStatus() != ValidationStatus.WAITING_RECIPIENT_VALIDATION) {
            return ResponseEntity.status(409).body(Map.of("error", "Cette commande a déjà été validée."));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("packageDescription", request.getPackageDescription() != null ? request.getPackageDescription() : "Colis sans description");
        response.put("origin", request.getOrigin());
        response.put("senderName", request.getClient() != null ? request.getClient().getFullName() : "Woyo Client");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{token}")
    public ResponseEntity<?> validateLocation(@PathVariable String token, @RequestBody LocationPayload payload) {
        if (payload.getLatitude() == null || payload.getLongitude() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Les coordonnées GPS sont obligatoires."));
        }
        
        try {
            String qrCode = rideRequestService.validateRecipientLocation(token, payload.getLatitude(), payload.getLongitude());
            return ResponseEntity.ok(Map.of("qrCodeDelivery", qrCode));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de la validation : " + e.getMessage()));
        }
    }
    
    public static class LocationPayload {
        private Double latitude;
        private Double longitude;

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }
    }
}
