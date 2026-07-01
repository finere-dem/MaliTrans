package com.malitrans.transport.controller;

import com.malitrans.transport.dto.LocationMessage;
import com.malitrans.transport.model.RideRequest;
import com.malitrans.transport.model.ValidationStatus;
import com.malitrans.transport.service.RideRequestService;
import com.malitrans.transport.service.TrackingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/public/validate")
@CrossOrigin(origins = "*")
public class PublicValidationController {

    private final RideRequestService rideRequestService;
    private final TrackingService trackingService;

    public PublicValidationController(RideRequestService rideRequestService, TrackingService trackingService) {
        this.rideRequestService = rideRequestService;
        this.trackingService = trackingService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getPublicRideInfo(@PathVariable String token) {
        try {
            return ResponseEntity.ok(rideRequestService.getRecipientValidationInfo(token));
        } catch (RideRequestService.LinkExpiredException e) {
            return expiredLinkResponse(e);
        }
    }

    @PostMapping("/{token}")
    public ResponseEntity<?> validateLocation(@PathVariable String token, @RequestBody LocationPayload payload) {
        if (payload.getLatitude() == null || payload.getLongitude() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Les coordonnees GPS sont obligatoires."));
        }

        try {
            RideRequest request = rideRequestService.validateRecipientLocation(
                    token,
                    payload.getLatitude(),
                    payload.getLongitude());
            return ResponseEntity.ok(toPublicRideInfo(request));
        } catch (RideRequestService.LinkExpiredException e) {
            return expiredLinkResponse(e);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de la validation : " + e.getMessage()));
        }
    }

    @GetMapping("/{token}/tracking")
    public ResponseEntity<?> getPublicTrackingInfo(@PathVariable String token) {
        try {
            rideRequestService.getRecipientValidationInfo(token);
        } catch (RideRequestService.LinkExpiredException e) {
            return expiredLinkResponse(e);
        }

        Optional<RideRequest> requestOpt = rideRequestService.getRideRequestByValidationToken(token);
        if (requestOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Lien de validation invalide ou expire."));
        }

        return ResponseEntity.ok(toPublicRideInfo(requestOpt.get()));
    }

    @GetMapping("/tracking/{rideId}")
    public ResponseEntity<?> getPublicTrackingInfoByCode(@PathVariable Long rideId, @RequestParam String code) {
        Optional<RideRequest> requestOpt = rideRequestService.getRideRequestEntityById(rideId);

        if (requestOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Course introuvable."));
        }

        RideRequest request = requestOpt.get();
        if (request.getQrCodeDelivery() == null || !request.getQrCodeDelivery().equals(code)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acces au suivi refuse."));
        }

        return ResponseEntity.ok(toPublicRideInfo(request));
    }

    private ResponseEntity<?> expiredLinkResponse(RideRequestService.LinkExpiredException e) {
        return ResponseEntity.status(410).body(Map.of(
                "error", "LINK_EXPIRED",
                "message", e.getMessage()));
    }

    private Map<String, Object> toPublicRideInfo(RideRequest request) {
        ValidationStatus status = request.getValidationStatus();
        boolean waitingForRecipient = status == ValidationStatus.WAITING_RECIPIENT_VALIDATION;
        boolean terminal = status == ValidationStatus.COMPLETED || status == ValidationStatus.CANCELED;

        Map<String, Object> response = new HashMap<>();
        response.put("rideId", request.getId());
        response.put("validationStatus", status != null ? status.name() : null);
        response.put("canValidateLocation", waitingForRecipient);
        response.put("trackingEnabled", !waitingForRecipient && !terminal);
        response.put("driverAssigned", request.getChauffeur() != null);
        response.put("price", request.getPrice());
        response.put("packageDescription", request.getPackageDescription() != null
                ? request.getPackageDescription()
                : "Colis sans description");
        response.put("origin", request.getOrigin());
        response.put("destination", request.getDestination());
        response.put("senderName", resolveSenderName(request));
        response.put("recipientName", resolveRecipientName(request));
        response.put("qrCodeDelivery", request.getQrCodeDelivery());
        response.put("trackingToken", request.getQrCodeDelivery());

        trackingService.getLastLocation(request.getId()).ifPresent(location ->
                response.put("lastLocation", toLocationMap(location)));

        return response;
    }

    private String resolveSenderName(RideRequest request) {
        if (Boolean.FALSE.equals(request.getIsSenderClient())) {
            if (request.getOtherPartyName() != null && !request.getOtherPartyName().isBlank()) {
                return request.getOtherPartyName();
            }
            if (request.getSupplier() != null) {
                return request.getSupplier().getFullName();
            }
        }
        return request.getClient() != null ? request.getClient().getFullName() : "Woyo Client";
    }

    private String resolveRecipientName(RideRequest request) {
        if (Boolean.TRUE.equals(request.getIsSenderClient())) {
            if (request.getOtherPartyName() != null && !request.getOtherPartyName().isBlank()) {
                return request.getOtherPartyName();
            }
            return "Destinataire";
        }
        return request.getClient() != null ? request.getClient().getFullName() : "Destinataire";
    }

    private Map<String, Object> toLocationMap(LocationMessage location) {
        Map<String, Object> map = new HashMap<>();
        map.put("rideId", location.getRideId());
        map.put("latitude", location.getLatitude());
        map.put("longitude", location.getLongitude());
        map.put("timestamp", location.getTimestamp());
        return map;
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
