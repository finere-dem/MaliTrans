package com.malitrans.transport.service;

import com.malitrans.transport.dto.PaginatedResponse;
import com.malitrans.transport.dto.RideRequestDTO;
import com.malitrans.transport.exception.RideAlreadyTakenException;
import com.malitrans.transport.mapper.RideRequestMapper;
import com.malitrans.transport.model.FlowType;
import com.malitrans.transport.model.RideRequest;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.model.ValidationStatus;
import com.malitrans.transport.repository.RideRequestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RideRequestService {

    private static final Logger logger = LoggerFactory.getLogger(RideRequestService.class);

    private final RideRequestRepository repository;
    private final UtilisateurService utilisateurService;
    private final RideRequestMapper mapper;
    private final NotificationService notificationService;
    private final String googleMapsApiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public RideRequestService(RideRequestRepository repository, 
                             UtilisateurService utilisateurService, 
                             RideRequestMapper mapper,
                             NotificationService notificationService,
                             @Value("${google.maps.api-key:}") String googleMapsApiKey) {
        this.repository = repository;
        this.utilisateurService = utilisateurService;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.googleMapsApiKey = googleMapsApiKey != null ? googleMapsApiKey.trim() : "";
    }

    /**
     * Create a new ride request according to the P2P (Peer-to-Peer) model
     * For CLIENT_INITIATED: Sets status directly to READY_FOR_PICKUP (no supplier validation needed)
     * For SUPPLIER_INITIATED: Sets status to WAITING_CLIENT_VALIDATION (client must validate)
     * @param dto The ride request DTO (clientId auto-set from JWT, supplierId is optional/nullable)
     * @param currentUserId The current authenticated user's ID (extracted from JWT)
     * @param currentUserRole The current authenticated user's role (extracted from JWT)
     */
    @Transactional
    public RideRequestDTO createRideRequest(RideRequestDTO dto, Long currentUserId, com.malitrans.transport.model.Role currentUserRole) {
        RideRequest entity = mapper.toEntity(dto);
        
        // CRITICAL: Explicitly set chauffeur and supplier to null for new requests
        // This prevents Hibernate TransientPropertyValueException when MapStruct creates empty Utilisateur objects
        entity.setChauffeur(null);
        entity.setSupplier(null); // P2P Model: Always null - contact details stored in otherPartyName/Phone
        
        // SECURITY: Auto-set clientId based on authenticated user's role
        Utilisateur currentUser = utilisateurService.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found with ID: " + currentUserId));
        
        if (currentUserRole == com.malitrans.transport.model.Role.CLIENT) {
            // Client is creating the request - auto-set clientId
            entity.setClient(currentUser);
            // P2P Model: Supplier is NOT linked as a User entity
            // Contact details are stored in otherPartyName and otherPartyPhone fields
            // supplier field remains null to avoid TransientPropertyValueException
        } else if (currentUserRole == com.malitrans.transport.model.Role.SUPPLIER) {
            // Supplier is creating the request - auto-set supplierId
            entity.setSupplier(currentUser);
            // Client ID must be provided in DTO
            if (dto.getClientId() == null) {
                throw new IllegalArgumentException("Client ID is required when creating a request as a SUPPLIER");
            }
        } else {
            throw new IllegalArgumentException("Only CLIENT or SUPPLIER can create ride requests");
        }
        
        // Set client (if not already set above)
        if (entity.getClient() == null && dto.getClientId() != null) {
            Utilisateur client = utilisateurService.findById(dto.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("Client not found with ID: " + dto.getClientId()));
            entity.setClient(client);
        }
        
        // Set flowType
        FlowType flowType;
        if (dto.getFlowType() != null) {
            try {
                flowType = FlowType.valueOf(dto.getFlowType());
                entity.setFlowType(flowType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid flowType: " + dto.getFlowType());
            }
        } else {
            throw new IllegalArgumentException("flowType is required");
        }
        
        // Flow initialization logic - P2P Model: Client requests
        if (flowType == FlowType.CLIENT_INITIATED) {
            // Check if destination is provided. If not, it's waiting for recipient validation.
            boolean hasDestination = dto.getDestination() != null && !dto.getDestination().trim().isEmpty();
            
            if (!hasDestination) {
                // Destination is unknown, waiting for recipient validation
                entity.setValidationStatus(ValidationStatus.WAITING_RECIPIENT_VALIDATION);
                // Generation of QR code delivery is done here too, but validation token is generated in PrePersist
                entity.setQrCodeDelivery(generateQrCode());
                entity.setQrCodePickup(generateQrCode());
                
                RideRequest saved = repository.save(entity);
                return mapper.toDto(saved);
            } else {
                // Destination known, go directly to READY_FOR_PICKUP
                entity.setValidationStatus(ValidationStatus.READY_FOR_PICKUP);
                
                // Generate both QR codes immediately (for pickup and delivery)
                entity.setQrCodePickup(generateQrCode());
                entity.setQrCodeDelivery(generateQrCode());
                
                RideRequest saved = repository.save(entity);
                
                // Notify all drivers immediately
                notificationService.notifyDriversOfReadyRequest(saved);
                
                return mapper.toDto(saved);
            }
            
        } else if (flowType == FlowType.SUPPLIER_INITIATED) {
            // Supplier is already set above (from currentUser)
            // Ensure client is linked (should already be set above, but validate)
            if (entity.getClient() == null) {
                throw new IllegalArgumentException("Client must be linked for SUPPLIER_INITIATED flow");
            }
            
            // Set status to WAITING_CLIENT_VALIDATION
            entity.setValidationStatus(ValidationStatus.WAITING_CLIENT_VALIDATION);
            
            // Generate QR code for delivery (will be used by client for validation)
            entity.setQrCodeDelivery(generateQrCode());
            
            RideRequest saved = repository.save(entity);
            
            // Notify Client for validation
            notificationService.notifyClientForValidation(saved);
            
            return mapper.toDto(saved);
        }
        
        throw new IllegalArgumentException("Invalid flowType: " + flowType);
    }

    /**
     * Get all ride requests ready for pickup (for drivers to see available deliveries)
     * Returns most recent requests first (LIFO - Last In First Out)
     */
    public List<RideRequestDTO> getReadyForPickupRequests() {
        return repository.findByValidationStatusOrderByCreatedAtDesc(ValidationStatus.READY_FOR_PICKUP)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a ride request by ID
     * @param id The ride request ID
     * @return Optional containing the ride request DTO if found
     */
    public Optional<RideRequestDTO> getRideRequestById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto);
    }
    
    /**
     * Get a ride request by validation token
     * @param token The validation token String
     * @return Optional containing the ride request entity if found
     */
    public Optional<RideRequest> getRideRequestByValidationToken(String token) {
        return repository.findByValidationToken(token);
    }
    
    /**
     * Complete recipient validation via public web link
     */
    @Transactional
    public RideRequest validateRecipientLocation(String token, Double latitude, Double longitude) {
        RideRequest request = repository.findByValidationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable ou jeton invalide"));
                
        if (request.getValidationStatus() != ValidationStatus.WAITING_RECIPIENT_VALIDATION) {
            throw new IllegalStateException("Le statut de cette commande ne nécessite pas de validation destinataire.");
        }
        
        // Store a readable address when possible, with coordinates as a safe fallback.
        request.setDestination(resolveAddressFromCoordinates(latitude, longitude));
        if (request.getQrCodeDelivery() == null || request.getQrCodeDelivery().isBlank()) {
            request.setQrCodeDelivery(generateQrCode());
        }
        if (request.getQrCodePickup() == null || request.getQrCodePickup().isBlank()) {
            request.setQrCodePickup(generateQrCode());
        }
        
        // Shift to READY_FOR_PICKUP
        request.setValidationStatus(ValidationStatus.READY_FOR_PICKUP);
        
        RideRequest saved = repository.save(request);
        
        // Notify drivers that a new ride is available
        notificationService.notifyDriversOfReadyRequest(saved);
        
        return saved;
    }

    private String resolveAddressFromCoordinates(Double latitude, Double longitude) {
        String fallback = formatCoordinates(latitude, longitude);
        if (googleMapsApiKey.isBlank()) {
            logger.warn("GOOGLE_MAPS_API_KEY is missing; keeping destination as coordinates");
            return fallback;
        }

        try {
            String latLng = latitude + "," + longitude;
            String encodedLatLng = URLEncoder.encode(latLng, StandardCharsets.UTF_8);
            String encodedKey = URLEncoder.encode(googleMapsApiKey, StandardCharsets.UTF_8);
            URI uri = URI.create(
                    "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                            + encodedLatLng
                            + "&language=fr&key="
                            + encodedKey);

            HttpRequest httpRequest = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn("Google reverse geocoding failed with HTTP status {}", response.statusCode());
                return fallback;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String status = root.path("status").asText();
            JsonNode results = root.path("results");
            if (!"OK".equals(status) || !results.isArray() || results.isEmpty()) {
                logger.warn("Google reverse geocoding returned status {}", status);
                return fallback;
            }

            String formattedAddress = results.get(0).path("formatted_address").asText("").trim();
            return formattedAddress.isEmpty() ? fallback : formattedAddress;
        } catch (Exception e) {
            logger.warn("Unable to reverse geocode recipient location: {}", e.getMessage());
            return fallback;
        }
    }

    private String formatCoordinates(Double latitude, Double longitude) {
        return latitude + "," + longitude;
    }

    /**
     * Validate a ride request (by Client or Supplier)
     * Changes status to READY_FOR_PICKUP, generates QR codes, and broadcasts to drivers
     * @param requestId The ride request ID
     * @param userId The user ID validating (extracted from JWT) - must be the client or supplier
     */
    @Transactional
    public RideRequestDTO validateRequest(Long requestId, Long userId) {
        RideRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Ride request not found with ID: " + requestId));
        
        // SECURITY: Verify the user is authorized to validate this request
        boolean isAuthorized = false;
        if (request.getClient() != null && request.getClient().getId().equals(userId)) {
            isAuthorized = true;
        }
        if (request.getSupplier() != null && request.getSupplier().getId().equals(userId)) {
            isAuthorized = true;
        }
        
        if (!isAuthorized) {
            throw new org.springframework.security.access.AccessDeniedException(
                "You are not authorized to validate this ride request. Only the client or supplier can validate.");
        }
        
        // Validate current status
        if (request.getValidationStatus() != ValidationStatus.WAITING_SUPPLIER_VALIDATION 
                && request.getValidationStatus() != ValidationStatus.WAITING_CLIENT_VALIDATION) {
            throw new IllegalStateException("Request cannot be validated. Current status: " + request.getValidationStatus());
        }
        
        // Change status to READY_FOR_PICKUP
        request.setValidationStatus(ValidationStatus.READY_FOR_PICKUP);
        
        // Generate QR codes if not already generated
        if (request.getQrCodePickup() == null) {
            request.setQrCodePickup(generateQrCode());
        }
        if (request.getQrCodeDelivery() == null) {
            request.setQrCodeDelivery(generateQrCode());
        }
        
        RideRequest saved = repository.save(request);
        
        // Broadcast to all drivers
        notificationService.notifyDriversOfReadyRequest(saved);
        
        return mapper.toDto(saved);
    }

    /**
     * Scan QR code for pickup or delivery
     * @param requestId The ride request ID
     * @param driverId The driver scanning the QR code (extracted from JWT)
     * @param qrCode The scanned QR code
     * @param type "PICKUP" or "DELIVERY"
     * @deprecated Use validatePickup() and validateDelivery() instead for proper state machine transitions
     */
    @Transactional
    @Deprecated
    public RideRequestDTO scanQrCode(Long requestId, Long driverId, String qrCode, String type) {
        RideRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Ride request not found with ID: " + requestId));
        
        // Validate driver is assigned to this request
        if (request.getChauffeur() == null || !request.getChauffeur().getId().equals(driverId)) {
            throw new IllegalStateException("Driver is not assigned to this ride request");
        }
        
        if ("PICKUP".equalsIgnoreCase(type)) {
            // Validate pickup QR code
            if (request.getQrCodePickup() == null || !request.getQrCodePickup().equals(qrCode)) {
                throw new IllegalArgumentException("Invalid pickup QR code");
            }
            
            // Use new state machine: DRIVER_ACCEPTED → IN_TRANSIT
            if (request.getValidationStatus() != ValidationStatus.IN_TRANSIT) {
                if (request.getValidationStatus() != ValidationStatus.DRIVER_ACCEPTED) {
                    throw new IllegalStateException("Pickup can only be validated when status is DRIVER_ACCEPTED. Current status: " + request.getValidationStatus());
                }
                request.setValidationStatus(ValidationStatus.IN_TRANSIT);
            }
            
        } else if ("DELIVERY".equalsIgnoreCase(type)) {
            // Validate delivery QR code
            if (request.getQrCodeDelivery() == null || !request.getQrCodeDelivery().equals(qrCode)) {
                throw new IllegalArgumentException("Invalid delivery QR code");
            }
            
            // Use new state machine: IN_TRANSIT → COMPLETED
            if (request.getValidationStatus() != ValidationStatus.IN_TRANSIT) {
                throw new IllegalStateException("Delivery can only be validated when status is IN_TRANSIT. Current status: " + request.getValidationStatus());
            }
            request.setValidationStatus(ValidationStatus.COMPLETED);
            
            // TODO: Trigger completion logic (payment, rating, etc.)
            
        } else {
            throw new IllegalArgumentException("Invalid QR scan type. Must be 'PICKUP' or 'DELIVERY'");
        }
        
        RideRequest saved = repository.save(request);
        return mapper.toDto(saved);
    }

    /**
     * Generate a unique 6-digit QR code
     */
    private String generateQrCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // 6-digit code (100000-999999)
        return String.valueOf(code);
    }

    /**
     * Assign a driver to a ride request (First-Come-First-Served)
     * CRITICAL: Uses @Transactional with optimistic locking to prevent race conditions
     */
    @Transactional
    public RideRequestDTO assignDriver(Long requestId, Long driverId) {
        // Load request within transaction
        RideRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Ride request not found with ID: " + requestId));
        
        // CRITICAL: Check if ride is still READY_FOR_PICKUP (concurrency control)
        if (request.getValidationStatus() != ValidationStatus.READY_FOR_PICKUP) {
            throw new RideAlreadyTakenException("Ride request is no longer available. Current status: " + request.getValidationStatus());
        }
        
        // Check if already assigned (double-check)
        if (request.getChauffeur() != null) {
            throw new RideAlreadyTakenException(requestId);
        }
        
        // Load driver
        Utilisateur driver = utilisateurService.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));
        
        // CRITICAL: Validate driver status - must be ACTIVE
        if (driver.getStatus() == null || driver.getStatus() != com.malitrans.transport.model.UserStatus.ACTIVE) {
            throw new IllegalStateException("Driver account is not active yet. Current status: " + 
                (driver.getStatus() != null ? driver.getStatus().name() : "NULL") + 
                ". Please wait for admin validation.");
        }
        
        // Assign driver (First-Come-First-Served)
        request.setChauffeur(driver);
        request.setValidationStatus(ValidationStatus.DRIVER_ACCEPTED); // State machine: READY_FOR_PICKUP → DRIVER_ACCEPTED
        
        RideRequest saved = repository.save(request);
        
        // Notify driver of assignment
        notificationService.notifyDriverOfAssignment(saved);

        // Notify client that their ride has been accepted by a driver
        notificationService.notifyClientOfDriverAccepted(saved);
        
        return mapper.toDto(saved);
    }

    /**
     * Validate pickup - transitions from DRIVER_ACCEPTED to IN_TRANSIT
     * @param requestId The ride request ID
     * @param driverId The driver validating pickup (extracted from JWT)
     * @param code The validation code to check against QR code pickup
     */
    @Transactional
    public RideRequestDTO validatePickup(Long requestId, Long driverId, String code) {
        RideRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Ride request not found with ID: " + requestId));
        
        // Validate driver is assigned to this request
        if (request.getChauffeur() == null || !request.getChauffeur().getId().equals(driverId)) {
            throw new IllegalStateException("Driver is not assigned to this ride request");
        }
        
        // State machine validation: Must be in DRIVER_ACCEPTED state
        if (request.getValidationStatus() != ValidationStatus.DRIVER_ACCEPTED) {
            throw new IllegalStateException("Pickup can only be validated when status is DRIVER_ACCEPTED. Current status: " + request.getValidationStatus());
        }
        
        // Validate code matches QR code pickup
        if (request.getQrCodePickup() == null || !request.getQrCodePickup().equals(code)) {
            throw new IllegalArgumentException("Code incorrect");
        }
        
        // Transition: DRIVER_ACCEPTED → IN_TRANSIT
        request.setValidationStatus(ValidationStatus.IN_TRANSIT);
        
        RideRequest saved = repository.save(request);
        return mapper.toDto(saved);
    }

    /**
     * Validate delivery by scan - transitions from IN_TRANSIT to COMPLETED
     */
    @Transactional
    public RideRequestDTO validateDeliveryByScan(Long requestId, Long driverId, String code) {
        RideRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Ride request not found with ID: " + requestId));
        
        if (request.getChauffeur() == null || !request.getChauffeur().getId().equals(driverId)) {
            throw new IllegalStateException("Driver is not assigned to this ride request");
        }
        
        if (request.getValidationStatus() != ValidationStatus.IN_TRANSIT) {
            throw new IllegalStateException("Delivery can only be validated when status is IN_TRANSIT. Current status: " + request.getValidationStatus());
        }
        
        if (request.getQrCodeDelivery() == null || !request.getQrCodeDelivery().equals(code)) {
            throw new IllegalArgumentException("Code QR incorrect");
        }
        
        request.setValidationStatus(ValidationStatus.COMPLETED);
        
        RideRequest saved = repository.save(request);
        return mapper.toDto(saved);
    }

    /**
     * Validate delivery by phone number - transitions from IN_TRANSIT to COMPLETED
     */
    @Transactional
    public RideRequestDTO validateDeliveryByPhone(Long requestId, Long driverId, String phone) {
        RideRequest request = repository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Ride request not found with ID: " + requestId));
                
        if (request.getChauffeur() == null || !request.getChauffeur().getId().equals(driverId)) {
            throw new IllegalStateException("Driver is not assigned to this ride request");
        }
        
        if (request.getValidationStatus() != ValidationStatus.IN_TRANSIT) {
            throw new IllegalStateException("Delivery can only be validated when status is IN_TRANSIT. Current status: " + request.getValidationStatus());
        }
        
        // Find the correct recipient phone number
        String recipientPhone = Boolean.TRUE.equals(request.getIsSenderClient()) 
            ? request.getOtherPartyPhone() 
            : (request.getClient() != null ? request.getClient().getPhone() : "");
            
        if (recipientPhone == null || recipientPhone.trim().isEmpty()) {
            throw new IllegalArgumentException("Aucun numéro de téléphone destinataire enregistré pour cette course");
        }
        
        // Clean both phone numbers from white spaces for a robust check
        String cleanRecipient = recipientPhone.replaceAll("\\s+", "");
        String cleanProvided = phone != null ? phone.replaceAll("\\s+", "") : "";
        
        if (!cleanRecipient.equals(cleanProvided)) {
            throw new IllegalArgumentException("Numéro de téléphone incorrect");
        }
        
        request.setValidationStatus(ValidationStatus.COMPLETED);
        
        RideRequest saved = repository.save(request);
        return mapper.toDto(saved);
    }
    
    /**
     * Legacy method for backward compatibility
     */
    @Transactional
    public RideRequestDTO validateDelivery(Long requestId, Long driverId, String code) {
        return validateDeliveryByScan(requestId, driverId, code);
    }

    /**
     * Get history for a client, ordered by creation date descending (most recent first)
     * @param clientId The client ID
     * @return List of ride request DTOs, most recent first
     */
    public List<RideRequestDTO> historyForClient(Long clientId) {
        return utilisateurService.findById(clientId)
                .map(repository::findByClientOrderByCreatedAtDesc)
                .orElse(List.of())
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get history for a supplier, ordered by creation date descending (most recent first)
     * @param supplierId The supplier ID
     * @return List of ride request DTOs, most recent first
     */
    public List<RideRequestDTO> historyForSupplier(Long supplierId) {
        return utilisateurService.findById(supplierId)
                .map(repository::findBySupplierOrderByCreatedAtDesc)
                .orElse(List.of())
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get paginated history for a driver
     * @param chauffeurId The driver ID
     * @param page Page number (1-based, default: 1)
     * @param limit Number of items per page (default: 20)
     * @return Paginated response with ride requests and metadata
     */
    public PaginatedResponse<RideRequestDTO> historyForChauffeur(Long chauffeurId, int page, int limit) {
        // Validate and normalize page and limit
        if (page < 1) {
            page = 1;
        }
        if (limit < 1) {
            limit = 20;
        }
        
        // Convert 1-based page to 0-based for Spring Data
        // Apply descending sort by createdAt (most recent first)
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        
        // Find chauffeur or return empty response
        Optional<Utilisateur> chauffeurOpt = utilisateurService.findById(chauffeurId);
        if (chauffeurOpt.isEmpty()) {
            return new PaginatedResponse<>(
                    new java.util.ArrayList<>(),
                    new PaginatedResponse.Meta(0, page, 0, limit)
            );
        }
        
        Utilisateur chauffeur = chauffeurOpt.get();
        
        // Retrieve paginated results from repository
        Page<RideRequest> pageResult = repository.findCompletedOrCanceledByChauffeurOrderByCreatedAtDesc(
                chauffeur, 
                List.of(ValidationStatus.COMPLETED, ValidationStatus.CANCELED),
                pageable);
        
        // Convert entities to DTOs with explicit typing
        List<RideRequestDTO> dtos = pageResult.getContent().stream()
                .map(entity -> {
                    RideRequestDTO dto = mapper.toDto(entity);
                    // IMPORTANT: Force the mapping of createdAt if mapper missed it
                    if (dto.getCreatedAt() == null) {
                        if (entity.getCreatedAt() != null) {
                            dto.setCreatedAt(entity.getCreatedAt());
                        } else {
                            // Fallback to current time if null in DB to avoid breaking frontend
                            dto.setCreatedAt(java.time.LocalDateTime.now());
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        
        // Handle case where requested page exceeds total pages
        int totalPages = pageResult.getTotalPages();
        if (page > totalPages && totalPages > 0) {
            // Return empty data with correct metadata
            return new PaginatedResponse<>(
                    new java.util.ArrayList<>(),
                    new PaginatedResponse.Meta(
                            pageResult.getTotalElements(),
                            page,
                            totalPages,
                            limit
                    )
            );
        }
        
        // Create metadata
        PaginatedResponse.Meta meta = new PaginatedResponse.Meta(
                pageResult.getTotalElements(),
                page,
                totalPages,
                limit
        );
        
        // Return paginated response with correctly typed DTOs
        return new PaginatedResponse<>(dtos, meta);
    }

    /**
     * Get all active rides for a driver (DRIVER_ACCEPTED or IN_TRANSIT)
     * Supports batching - a driver can have multiple active rides simultaneously
     * @param chauffeurId The driver ID
     * @return List of active ride request DTOs, empty list if none
     */
    public List<RideRequestDTO> getActiveRidesForChauffeur(Long chauffeurId) {
        // Find chauffeur or return empty list
        Optional<Utilisateur> chauffeurOpt = utilisateurService.findById(chauffeurId);
        if (chauffeurOpt.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        Utilisateur chauffeur = chauffeurOpt.get();
        
        // Retrieve active rides (DRIVER_ACCEPTED or IN_TRANSIT)
        List<RideRequest> activeRides = repository.findActiveByChauffeurOrderByCreatedAtDesc(
                chauffeur,
                List.of(ValidationStatus.DRIVER_ACCEPTED, ValidationStatus.IN_TRANSIT));
        
        // Convert entities to DTOs with createdAt null handling
        return activeRides.stream()
                .map(entity -> {
                    RideRequestDTO dto = mapper.toDto(entity);
                    // Ensure createdAt is never null
                    if (dto.getCreatedAt() == null) {
                        if (entity.getCreatedAt() != null) {
                            dto.setCreatedAt(entity.getCreatedAt());
                        } else {
                            // Fallback to current time if null in DB
                            dto.setCreatedAt(java.time.LocalDateTime.now());
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Annuler une course (Client uniquement)
     */
    @Transactional
    public RideRequestDTO cancelRide(Long rideId, Long clientId) {
        RideRequest request = repository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Course introuvable avec l'ID: " + rideId));

        // Sécurité : Vérifier que l'utilisateur est bien le propriétaire
        if (request.getClient() == null || !request.getClient().getId().equals(clientId)) {
            throw new org.springframework.security.access.AccessDeniedException("Vous n'êtes pas autorisé à annuler cette course.");
        }

        // Sécurité : On ne peut annuler que si aucun chauffeur n'a accepté
        if (request.getValidationStatus() != ValidationStatus.READY_FOR_PICKUP &&
                request.getValidationStatus() != ValidationStatus.WAITING_RECIPIENT_VALIDATION &&
                request.getValidationStatus() != ValidationStatus.WAITING_CLIENT_VALIDATION &&
                request.getValidationStatus() != ValidationStatus.WAITING_SUPPLIER_VALIDATION) {
            throw new IllegalStateException("Impossible d'annuler : la course a déjà été acceptée ou est en cours.");
        }

        request.setValidationStatus(ValidationStatus.CANCELED);
        return mapper.toDto(repository.save(request));
    }

    /**
     * Modifier le prix d'une course (Client uniquement)
     */
    @Transactional
    public RideRequestDTO updateRidePrice(Long rideId, Long clientId, Double newPrice) {
        RideRequest request = repository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Course introuvable avec l'ID: " + rideId));

        // Sécurité : Vérifier que l'utilisateur est bien le propriétaire
        if (request.getClient() == null || !request.getClient().getId().equals(clientId)) {
            throw new org.springframework.security.access.AccessDeniedException("Vous n'êtes pas autorisé à modifier cette course.");
        }

        // Sécurité : On ne peut modifier que si aucun chauffeur n'a accepté
        if (request.getValidationStatus() != ValidationStatus.READY_FOR_PICKUP &&
                request.getValidationStatus() != ValidationStatus.WAITING_RECIPIENT_VALIDATION &&
                request.getValidationStatus() != ValidationStatus.WAITING_CLIENT_VALIDATION &&
                request.getValidationStatus() != ValidationStatus.WAITING_SUPPLIER_VALIDATION) {
            throw new IllegalStateException("Impossible de modifier le prix : la course a déjà été acceptée ou est en cours.");
        }

        request.setPrice(newPrice);
        return mapper.toDto(repository.save(request));
    }
    
    /**
     * Obtenir le lien de validation pour le destinataire
     */
    @Transactional
    public String getValidationLink(Long rideId, Long clientId, String baseUrl) {
        RideRequest request = repository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Course introuvable avec l'ID: " + rideId));
                
        if (request.getClient() == null || !request.getClient().getId().equals(clientId)) {
            throw new org.springframework.security.access.AccessDeniedException("Vous n'êtes pas autorisé à obtenir ce lien.");
        }

        boolean missingDestination = request.getDestination() == null || request.getDestination().trim().isEmpty();
        boolean terminalStatus = request.getValidationStatus() == ValidationStatus.COMPLETED ||
                request.getValidationStatus() == ValidationStatus.CANCELED;
        boolean canAskRecipient = request.getValidationStatus() == ValidationStatus.WAITING_RECIPIENT_VALIDATION ||
                (missingDestination && request.getChauffeur() == null && !terminalStatus);

        if (!canAskRecipient) {
            throw new IllegalStateException("Le statut de cette course ne permet pas l'envoi d'un lien de validation.");
        }

        if (request.getValidationStatus() != ValidationStatus.WAITING_RECIPIENT_VALIDATION) {
            request.setValidationStatus(ValidationStatus.WAITING_RECIPIENT_VALIDATION);
        }

        if (request.getValidationToken() == null || request.getValidationToken().isBlank()) {
            request.setValidationToken(UUID.randomUUID().toString());
        }

        repository.save(request);

        return baseUrl + "/validate.html?token=" + request.getValidationToken();
    }
}
