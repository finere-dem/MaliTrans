package com.malitrans.transport.service;

import com.malitrans.transport.dto.DriverDossierDTO;
import com.malitrans.transport.dto.DriverSummaryDTO;
import com.malitrans.transport.dto.GuarantorDTO;
import com.malitrans.transport.dto.PaginatedResponse;
import com.malitrans.transport.exception.ValidationException;
import com.malitrans.transport.model.Guarantor;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.model.UserStatus;
import com.malitrans.transport.repository.GuarantorRepository;
import com.malitrans.transport.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CompanyService {

    private final UtilisateurRepository utilisateurRepository;
    private final GuarantorRepository guarantorRepository;
    private static final int REQUIRED_GUARANTORS = 2;

    public CompanyService(UtilisateurRepository utilisateurRepository, GuarantorRepository guarantorRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.guarantorRepository = guarantorRepository;
    }

    /**
     * Validate a driver by company manager
     * Adds guarantors/documents and changes status to PENDING_ADMIN_APPROVAL
     * @param driverId The driver ID to validate
     * @param managerId The company manager ID (extracted from JWT)
     * @param guarantors List of guarantors to add
     */
    @Transactional
    public void validateDriver(Long driverId, Long managerId, List<GuarantorDTO> guarantors) {
        // Load driver
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        // Verify driver is a CHAUFFEUR
        if (driver.getRole() != Role.CHAUFFEUR) {
            throw new IllegalArgumentException("User is not a driver");
        }

        // Verify driver is in PENDING_COMPANY_VERIFICATION status
        if (driver.getStatus() != UserStatus.PENDING_COMPANY_VERIFICATION) {
            throw new IllegalStateException("Driver status must be PENDING_COMPANY_VERIFICATION. Current status: " + driver.getStatus());
        }

        // Load manager
        Utilisateur manager = utilisateurRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found with ID: " + managerId));

        // Verify manager is a COMPANY_MANAGER or SUPPLIER
        if (manager.getRole() != Role.COMPANY_MANAGER && manager.getRole() != Role.SUPPLIER) {
            throw new AccessDeniedException("Only company managers or suppliers can validate drivers");
        }

        // SECURITY: Verify manager and driver belong to the same company
        if (manager.getCompany() == null || driver.getCompany() == null) {
            throw new AccessDeniedException("Manager or driver does not belong to a company");
        }

        if (!manager.getCompany().getId().equals(driver.getCompany().getId())) {
            throw new AccessDeniedException("Manager can only validate drivers from their own company");
        }

        // Add guarantors if provided
        if (guarantors != null && !guarantors.isEmpty()) {
            // Delete existing guarantors
            List<Guarantor> existingGuarantors = guarantorRepository.findByDriver(driver);
            guarantorRepository.deleteAll(existingGuarantors);

            // Add new guarantors
            for (GuarantorDTO guarantorDTO : guarantors) {
                Guarantor guarantor = new Guarantor();
                guarantor.setName(guarantorDTO.getName());
                guarantor.setPhone(guarantorDTO.getPhone());
                guarantor.setAddress(guarantorDTO.getAddress());
                guarantor.setRelation(guarantorDTO.getRelation());
                guarantor.setIdentityDocumentUrl(guarantorDTO.getIdentityDocumentUrl());
                guarantor.setDriver(driver);
                guarantorRepository.save(guarantor);
            }
        }

        // Verify driver has required guarantors
        List<Guarantor> finalGuarantors = guarantorRepository.findByDriver(driver);
        if (finalGuarantors.size() < REQUIRED_GUARANTORS) {
            throw new ValidationException("MISSING_GUARANTORS",
                String.format("At least %d guarantors required. Currently has %d.", REQUIRED_GUARANTORS, finalGuarantors.size()));
        }

        // Verify driver has identity document
        if (driver.getIdentityDocumentUrl() == null || driver.getIdentityDocumentUrl().trim().isEmpty()) {
            throw new ValidationException("MISSING_DRIVER_ID_DOC", "Driver identity document missing");
        }

        // Generate and assign matricule (internal code) before saving
        // Only generate if not already set (to avoid regenerating on re-validation)
        if (driver.getMatricule() == null || driver.getMatricule().trim().isEmpty()) {
            String matricule = generateMatricule(driver, manager.getCompany().getName());
            driver.setMatricule(matricule);
        }

        // Transition: PENDING_COMPANY_VERIFICATION → PENDING_ADMIN_APPROVAL
        driver.setStatus(UserStatus.PENDING_ADMIN_APPROVAL);
        utilisateurRepository.save(driver);
    }

    /**
     * Generate a unique matricule (internal code) for a driver
     * Format: AAA-AAAA-NNNN
     * - AAA: First 3 letters of company name (uppercase, alphanumeric only)
     * - AAAA: Current year (e.g., 2024)
     * - NNNN: Last 4 digits of driver ID (padded with zeros if needed)
     * 
     * @param driver The driver to generate matricule for
     * @param companyName The name of the company
     * @return Generated matricule string
     */
    private String generateMatricule(Utilisateur driver, String companyName) {
        // Extract first 3 letters from company name (uppercase, alphanumeric only)
        String companyPrefix = companyName != null ? companyName.toUpperCase() : "COM";
        // Remove non-alphanumeric characters and take first 3 characters
        companyPrefix = companyPrefix.replaceAll("[^A-Z0-9]", "");
        if (companyPrefix.length() < 3) {
            // Pad with 'X' if company name is too short
            companyPrefix = String.format("%-3s", companyPrefix).replace(' ', 'X').substring(0, 3);
        } else {
            companyPrefix = companyPrefix.substring(0, 3);
        }
        
        // Get current year
        int currentYear = LocalDate.now().getYear();
        String year = String.valueOf(currentYear);
        
        // Get last 4 digits of driver ID (padded with zeros)
        String driverIdStr = String.valueOf(driver.getId());
        String driverSuffix;
        if (driverIdStr.length() >= 4) {
            driverSuffix = driverIdStr.substring(driverIdStr.length() - 4);
        } else {
            // Pad with zeros if ID is less than 4 digits
            driverSuffix = String.format("%04d", driver.getId());
        }
        
        // Format: AAA-AAAA-NNNN
        return String.format("%s-%s-%s", companyPrefix, year, driverSuffix);
    }

    /**
     * Get all drivers pending company verification for the manager's company
     * Uses efficient JPQL query instead of findAll().stream()
     * @param managerId The company manager ID (or supplier ID)
     * @return List of drivers
     */
    public List<Utilisateur> getPendingDriversForCompany(Long managerId) {
        Utilisateur manager = utilisateurRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found with ID: " + managerId));

        if (manager.getRole() != Role.COMPANY_MANAGER && manager.getRole() != Role.SUPPLIER) {
            throw new AccessDeniedException("Only company managers or suppliers can access this endpoint");
        }

        if (manager.getCompany() == null) {
            throw new IllegalStateException("Manager does not belong to a company");
        }

        // Use efficient JPQL query instead of findAll().stream()
        return utilisateurRepository.findDriversPendingForCompany(
                Role.CHAUFFEUR,
                manager.getCompany().getId(),
                UserStatus.PENDING_COMPANY_VERIFICATION
        );
    }

    /**
     * Get paginated list of drivers for a company manager's company
     * @param managerId The company manager ID (or supplier ID)
     * @param page Page number (1-based)
     * @param limit Number of items per page
     * @param status Optional status filter (null to ignore)
     * @param searchQuery Optional search query for username, phone, or matricule (null or empty to ignore)
     * @return PaginatedResponse with DriverSummaryDTO list
     */
    public PaginatedResponse<DriverSummaryDTO> getCompanyDrivers(
            Long managerId, int page, int limit, UserStatus status, String searchQuery) {
        
        // Validate and normalize page and limit
        if (page < 1) {
            page = 1;
        }
        if (limit < 1) {
            limit = 20;
        }
        if (limit > 100) {
            limit = 100; // Max limit to prevent performance issues
        }

        // Load manager
        Utilisateur manager = utilisateurRepository.findById(managerId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found with ID: " + managerId));

        // Verify manager is a COMPANY_MANAGER or SUPPLIER
        if (manager.getRole() != Role.COMPANY_MANAGER && manager.getRole() != Role.SUPPLIER) {
            throw new AccessDeniedException("Only company managers or suppliers can access this endpoint");
        }

        // Verify manager belongs to a company
        if (manager.getCompany() == null) {
            throw new IllegalStateException("Manager does not belong to a company");
        }

        Long companyId = manager.getCompany().getId();

        // Normalize search query (trim and set to null if empty)
        String normalizedSearchQuery = (searchQuery != null && !searchQuery.trim().isEmpty()) 
                ? searchQuery.trim() 
                : null;

        // Convert enum to String for native query
        String roleStr = Role.CHAUFFEUR.name();
        String statusStr = (status != null) ? status.name() : null;

        // Create Pageable (Spring Data uses 0-based page index)
        Pageable pageable = PageRequest.of(page - 1, limit);

        // Query drivers with pagination and filters using native query (PostgreSQL-safe)
        Page<Utilisateur> pageResult = utilisateurRepository.findDriversByCompanyNative(
                roleStr,
                companyId,
                statusStr,
                normalizedSearchQuery,
                pageable
        );

        // Convert to DTOs
        List<DriverSummaryDTO> dtos = pageResult.getContent().stream()
                .map(driver -> {
                    DriverSummaryDTO dto = new DriverSummaryDTO();
                    dto.setId(driver.getId());
                    dto.setUsername(driver.getUsername());
                    dto.setFullName(driver.getFullName());
                    dto.setPhone(driver.getPhone());
                    dto.setStatus(driver.getStatus());
                    dto.setMatricule(driver.getMatricule());
                    // Note: Utilisateur doesn't have createdAt field, so we set it to null
                    // If you add createdAt to Utilisateur later, update this line
                    dto.setCreatedAt(null);
                    return dto;
                })
                .collect(Collectors.toList());

        // Calculate total pages
        int totalPages = pageResult.getTotalPages();

        // Create meta object with pageSize
        PaginatedResponse.Meta meta = new PaginatedResponse.Meta(
                pageResult.getTotalElements(),
                page,
                totalPages,
                limit
        );

        return new PaginatedResponse<>(dtos, meta);
    }

    /**
     * Get full driver dossier (summary + identity document + guarantors + status + matricule)
     * Only accessible by company managers/suppliers from the same company as the driver
     * @param driverId The driver ID
     * @param requesterId The ID of the requesting manager/supplier (extracted from JWT)
     * @return DriverDossierDTO with complete driver information
     */
    public DriverDossierDTO getDriverDossier(Long driverId, Long requesterId) {
        // Load driver
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        // Verify driver is a CHAUFFEUR
        if (driver.getRole() != Role.CHAUFFEUR) {
            throw new IllegalArgumentException("User is not a driver");
        }

        // Load requester
        Utilisateur requester = utilisateurRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("Requester not found with ID: " + requesterId));

        // Verify requester is a COMPANY_MANAGER or SUPPLIER
        if (requester.getRole() != Role.COMPANY_MANAGER && requester.getRole() != Role.SUPPLIER) {
            throw new AccessDeniedException("Only company managers or suppliers can access driver dossiers");
        }

        // SECURITY: Verify requester and driver belong to the same company
        if (requester.getCompany() == null || driver.getCompany() == null) {
            throw new AccessDeniedException("Requester or driver does not belong to a company");
        }

        if (!requester.getCompany().getId().equals(driver.getCompany().getId())) {
            throw new AccessDeniedException("Requester can only access drivers from their own company");
        }

        // Load guarantors
        List<Guarantor> guarantors = guarantorRepository.findByDriver(driver);
        List<GuarantorDTO> guarantorDTOs = guarantors.stream()
                .map(g -> {
                    GuarantorDTO dto = new GuarantorDTO();
                    dto.setId(g.getId());
                    dto.setName(g.getName());
                    dto.setPhone(g.getPhone());
                    dto.setAddress(g.getAddress());
                    dto.setRelation(g.getRelation());
                    dto.setIdentityDocumentUrl(g.getIdentityDocumentUrl());
                    dto.setDriverId(driver.getId());
                    return dto;
                })
                .collect(Collectors.toList());

        // Build dossier DTO
        DriverDossierDTO dossier = new DriverDossierDTO();
        dossier.setId(driver.getId());
        dossier.setUsername(driver.getUsername());
        dossier.setFullName(driver.getFullName());
        dossier.setPhone(driver.getPhone());
        dossier.setAddress(driver.getAddress());
        dossier.setVehicleType(driver.getVehicleType());
        dossier.setIdentityDocumentUrl(driver.getIdentityDocumentUrl());
        dossier.setMatricule(driver.getMatricule());
        dossier.setStatus(driver.getStatus());
        dossier.setCompanyName(driver.getCompany() != null ? driver.getCompany().getName() : null);
        dossier.setGuarantors(guarantorDTOs);

        return dossier;
    }
}

