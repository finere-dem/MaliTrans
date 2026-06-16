package com.malitrans.transport.service;

import com.malitrans.transport.dto.DriverValidationDTO;
import com.malitrans.transport.dto.GuarantorDTO;
import com.malitrans.transport.model.DeliveryCompany;
import com.malitrans.transport.model.Guarantor;
import com.malitrans.transport.model.RideRequest;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.model.UserStatus;
import com.malitrans.transport.model.ValidationStatus;
import com.malitrans.transport.repository.DeliveryCompanyRepository;
import com.malitrans.transport.repository.GuarantorRepository;
import com.malitrans.transport.repository.RideRequestRepository;
import com.malitrans.transport.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UtilisateurRepository utilisateurRepository;
    private final GuarantorRepository guarantorRepository;
    private final DeliveryCompanyRepository deliveryCompanyRepository;
    private final RideRequestRepository rideRequestRepository;

    public AdminService(UtilisateurRepository utilisateurRepository, GuarantorRepository guarantorRepository,
            DeliveryCompanyRepository deliveryCompanyRepository, RideRequestRepository rideRequestRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.guarantorRepository = guarantorRepository;
        this.deliveryCompanyRepository = deliveryCompanyRepository;
        this.rideRequestRepository = rideRequestRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOverview() {
        List<Utilisateur> users = utilisateurRepository.findAll();
        List<DeliveryCompany> companies = deliveryCompanyRepository.findAll();
        List<RideRequest> rides = rideRequestRepository.findAll();

        Map<String, Long> usersByRole = users.stream()
                .collect(Collectors.groupingBy(
                        user -> user.getRole() != null ? user.getRole().name() : "UNKNOWN",
                        Collectors.counting()));

        Map<String, Long> driversByStatus = users.stream()
                .filter(user -> user.getRole() == Role.CHAUFFEUR)
                .collect(Collectors.groupingBy(
                        user -> user.getStatus() != null ? user.getStatus().name() : "UNKNOWN",
                        Collectors.counting()));

        Map<String, Long> ridesByStatus = rides.stream()
                .collect(Collectors.groupingBy(
                        ride -> ride.getValidationStatus() != null ? ride.getValidationStatus().name() : "UNKNOWN",
                        Collectors.counting()));

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalUsers", users.size());
        overview.put("totalDrivers", users.stream().filter(user -> user.getRole() == Role.CHAUFFEUR).count());
        overview.put("totalClients", users.stream().filter(user -> user.getRole() == Role.CLIENT).count());
        overview.put("totalSuppliers", users.stream().filter(user -> user.getRole() == Role.SUPPLIER).count());
        overview.put("totalCompanies", companies.size());
        overview.put("activeCompanies", companies.stream().filter(DeliveryCompany::isActive).count());
        overview.put("totalRides", rides.size());
        overview.put("pendingAdminDrivers", driversByStatus.getOrDefault(UserStatus.PENDING_ADMIN_APPROVAL.name(), 0L)
                + driversByStatus.getOrDefault(UserStatus.PENDING_VALIDATION.name(), 0L));
        overview.put("activeDrivers", driversByStatus.getOrDefault(UserStatus.ACTIVE.name(), 0L));
        overview.put("usersByRole", usersByRole);
        overview.put("driversByStatus", driversByStatus);
        overview.put("ridesByStatus", ridesByStatus);
        return overview;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUsers(String role, String status, String q, int limit) {
        String normalizedRole = normalize(role);
        String normalizedStatus = normalize(status);
        String search = q != null ? q.trim().toLowerCase() : "";

        return utilisateurRepository.findAll().stream()
                .filter(user -> normalizedRole == null
                        || (user.getRole() != null && user.getRole().name().equals(normalizedRole)))
                .filter(user -> normalizedStatus == null
                        || (user.getStatus() != null && user.getStatus().name().equals(normalizedStatus)))
                .filter(user -> search.isEmpty() || contains(user.getUsername(), search)
                        || contains(user.getPhone(), search)
                        || contains(user.getEmail(), search)
                        || contains(user.getFullName(), search))
                .sorted(Comparator.comparing(Utilisateur::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .limit(limit)
                .map(this::toUserMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCompanies() {
        return deliveryCompanyRepository.findAll().stream()
                .sorted(Comparator.comparing(DeliveryCompany::getId, Comparator.nullsLast(Long::compareTo)))
                .map(company -> {
                    List<Utilisateur> drivers = company.getDrivers() != null ? company.getDrivers() : List.of();
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", company.getId());
                    map.put("name", company.getName());
                    map.put("address", company.getAddress());
                    map.put("active", company.isActive());
                    map.put("driverCount", drivers.stream().filter(user -> user.getRole() == Role.CHAUFFEUR).count());
                    map.put("activeDriverCount", drivers.stream()
                            .filter(user -> user.getRole() == Role.CHAUFFEUR && user.getStatus() == UserStatus.ACTIVE)
                            .count());
                    map.put("pendingDriverCount", drivers.stream()
                            .filter(user -> user.getRole() == Role.CHAUFFEUR
                                    && (user.getStatus() == UserStatus.PENDING_COMPANY_VERIFICATION
                                            || user.getStatus() == UserStatus.PENDING_ADMIN_APPROVAL
                                            || user.getStatus() == UserStatus.PENDING_VALIDATION))
                            .count());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createCompany(String name, String address) {
        String cleanName = name != null ? name.trim() : "";
        String cleanAddress = address != null ? address.trim() : "";

        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Le nom de la société est obligatoire.");
        }

        boolean alreadyExists = deliveryCompanyRepository.findAll().stream()
                .anyMatch(company -> company.getName() != null
                        && company.getName().trim().equalsIgnoreCase(cleanName));
        if (alreadyExists) {
            throw new IllegalStateException("Une société avec ce nom existe déjà.");
        }

        DeliveryCompany company = new DeliveryCompany(cleanName, cleanAddress);
        company.setActive(true);
        DeliveryCompany savedCompany = deliveryCompanyRepository.save(company);

        Map<String, Object> response = new HashMap<>();
        response.put("id", savedCompany.getId());
        response.put("name", savedCompany.getName());
        response.put("address", savedCompany.getAddress());
        response.put("active", savedCompany.isActive());
        response.put("driverCount", 0);
        response.put("activeDriverCount", 0);
        response.put("pendingDriverCount", 0);
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRides(String status, int limit) {
        String normalizedStatus = normalize(status);
        return rideRequestRepository.findAll().stream()
                .filter(ride -> normalizedStatus == null
                        || (ride.getValidationStatus() != null && ride.getValidationStatus().name().equals(normalizedStatus)))
                .sorted(Comparator.comparing(RideRequest::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(limit)
                .map(this::toRideMap)
                .collect(Collectors.toList());
    }

    /**
     * Get all drivers with PENDING_ADMIN_APPROVAL status (3-step flow)
     * @return List of drivers with their guarantors
     */
    public List<DriverValidationDTO> getPendingDrivers() {
        List<Utilisateur> pendingDrivers = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CHAUFFEUR 
                        && (u.getStatus() == UserStatus.PENDING_ADMIN_APPROVAL 
                            || u.getStatus() == UserStatus.PENDING_VALIDATION)) // Legacy support
                .collect(Collectors.toList());

        return pendingDrivers.stream()
                .map(driver -> {
                    DriverValidationDTO dto = new DriverValidationDTO();
                    dto.setId(driver.getId());
                    dto.setUsername(driver.getUsername());
                    dto.setFirstName(driver.getFirstName());
                    dto.setLastName(driver.getLastName());
                    dto.setPhone(driver.getPhone());
                    dto.setVehicleType(driver.getVehicleType());
                    dto.setIdentityDocumentUrl(driver.getIdentityDocumentUrl());
                    dto.setStatus(driver.getStatus() != null ? driver.getStatus().name() : null);

                    // Load guarantors
                    List<Guarantor> guarantors = guarantorRepository.findByDriver(driver);
                    List<GuarantorDTO> guarantorDTOs = guarantors.stream()
                            .map(g -> {
                                GuarantorDTO gDto = new GuarantorDTO();
                                gDto.setId(g.getId());
                                gDto.setName(g.getName());
                                gDto.setPhone(g.getPhone());
                                gDto.setAddress(g.getAddress());
                                gDto.setRelation(g.getRelation());
                                gDto.setIdentityDocumentUrl(g.getIdentityDocumentUrl());
                                gDto.setDriverId(driver.getId());
                                return gDto;
                            })
                            .collect(Collectors.toList());
                    dto.setGuarantors(guarantorDTOs);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> toUserMap(Utilisateur user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("fullName", user.getFullName());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("role", user.getRole() != null ? user.getRole().name() : null);
        map.put("status", user.getStatus() != null ? user.getStatus().name() : null);
        map.put("enabled", user.isEnabled());
        map.put("online", user.getIsOnline());
        map.put("companyId", user.getCompany() != null ? user.getCompany().getId() : null);
        map.put("companyName", user.getCompany() != null ? user.getCompany().getName() : user.getCompanyName());
        map.put("matricule", user.getMatricule());
        return map;
    }

    private Map<String, Object> toRideMap(RideRequest ride) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ride.getId());
        map.put("origin", ride.getOrigin());
        map.put("destination", ride.getDestination());
        map.put("flowType", ride.getFlowType() != null ? ride.getFlowType().name() : null);
        ValidationStatus validationStatus = ride.getValidationStatus();
        map.put("validationStatus", validationStatus != null ? validationStatus.name() : null);
        map.put("price", ride.getPrice());
        map.put("createdAt", ride.getCreatedAt());
        map.put("client", ride.getClient() != null ? ride.getClient().getUsername() : null);
        map.put("supplier", ride.getSupplier() != null ? ride.getSupplier().getUsername() : null);
        map.put("chauffeur", ride.getChauffeur() != null ? ride.getChauffeur().getUsername() : null);
        return map;
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    @Transactional
    public Map<String, Object> suspendUser(Long userId) {
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Admin users cannot be suspended from this dashboard.");
        }

        user.setEnabled(false);
        user.setStatus(UserStatus.SUSPENDED);
        utilisateurRepository.save(user);
        return toUserMap(user);
    }

    @Transactional
    public Map<String, Object> reactivateUser(Long userId) {
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Admin users cannot be modified from this dashboard.");
        }

        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        utilisateurRepository.save(user);
        return toUserMap(user);
    }

    @Transactional
    public Map<String, Object> setCompanyActive(Long companyId, boolean active) {
        DeliveryCompany company = deliveryCompanyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + companyId));

        company.setActive(active);
        deliveryCompanyRepository.save(company);

        Map<String, Object> response = new HashMap<>();
        response.put("id", company.getId());
        response.put("name", company.getName());
        response.put("active", company.isActive());
        return response;
    }

    /**
     * Activate a driver (change status to ACTIVE) - Final step in 3-step flow
     * @param driverId The driver's ID
     */
    @Transactional
    public void activateDriver(Long driverId) {
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        if (driver.getRole() != Role.CHAUFFEUR) {
            throw new IllegalArgumentException("User is not a driver");
        }

        // 3-step flow: Must be in PENDING_ADMIN_APPROVAL status
        if (driver.getStatus() != UserStatus.PENDING_ADMIN_APPROVAL) {
            // Legacy support: Also allow PENDING_VALIDATION for backward compatibility
            if (driver.getStatus() != UserStatus.PENDING_VALIDATION) {
                throw new IllegalStateException("Driver status must be PENDING_ADMIN_APPROVAL to be activated. Current status: " + driver.getStatus());
            }
        }

        driver.setStatus(UserStatus.ACTIVE);
        utilisateurRepository.save(driver);
    }

    /**
     * Validate a driver (change status to ACTIVE) - Legacy method for backward compatibility
     * @deprecated Use activateDriver instead
     * @param driverId The driver's ID
     */
    @Deprecated
    @Transactional
    public void validateDriver(Long driverId) {
        activateDriver(driverId);
    }

    /**
     * Reject a driver (change status to REJECTED)
     * @param driverId The driver's ID
     * @param reason Optional rejection reason
     */
    @Transactional
    public void rejectDriver(Long driverId, String reason) {
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        if (driver.getRole() != Role.CHAUFFEUR) {
            throw new IllegalArgumentException("User is not a driver");
        }

        // Can reject from PENDING_ADMIN_APPROVAL or PENDING_COMPANY_VERIFICATION
        if (driver.getStatus() != UserStatus.PENDING_ADMIN_APPROVAL 
                && driver.getStatus() != UserStatus.PENDING_COMPANY_VERIFICATION
                && driver.getStatus() != UserStatus.PENDING_VALIDATION) { // Legacy support
            throw new IllegalStateException("Driver status must be PENDING_ADMIN_APPROVAL or PENDING_COMPANY_VERIFICATION to be rejected. Current status: " + driver.getStatus());
        }

        driver.setStatus(UserStatus.REJECTED);
        utilisateurRepository.save(driver);
        
        // TODO: Store rejection reason if needed (could add a rejectionReason field to Utilisateur)
    }
}

