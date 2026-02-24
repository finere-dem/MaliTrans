package com.malitrans.transport.service;

import com.malitrans.transport.dto.DriverDossierDTO;
import com.malitrans.transport.dto.GuarantorDTO;
import com.malitrans.transport.model.Guarantor;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.model.UserStatus;
import com.malitrans.transport.repository.GuarantorRepository;
import com.malitrans.transport.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverService {

    private final GuarantorRepository guarantorRepository;
    private final UtilisateurRepository utilisateurRepository;
    private static final int REQUIRED_GUARANTORS = 2;

    public DriverService(GuarantorRepository guarantorRepository, UtilisateurRepository utilisateurRepository) {
        this.guarantorRepository = guarantorRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * Add a guarantor to the currently logged-in driver
     * 
     * @param driverId     The driver's ID (extracted from JWT)
     * @param guarantorDTO The guarantor information
     * @return The created guarantor DTO
     */
    @Transactional
    public GuarantorDTO addGuarantor(Long driverId, GuarantorDTO guarantorDTO) {
        // Load driver
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        // Verify user is a driver
        if (driver.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
            throw new IllegalArgumentException("User is not a driver");
        }

        // Check if driver already has 2 guarantors
        List<Guarantor> existingGuarantors = guarantorRepository.findByDriver(driver);
        if (existingGuarantors.size() >= REQUIRED_GUARANTORS) {
            throw new IllegalStateException(
                    "Driver already has " + REQUIRED_GUARANTORS + " guarantors. Maximum allowed.");
        }

        // Create new guarantor
        Guarantor guarantor = new Guarantor();
        guarantor.setName(guarantorDTO.getName());
        guarantor.setPhone(guarantorDTO.getPhone());
        guarantor.setAddress(guarantorDTO.getAddress());
        guarantor.setRelation(guarantorDTO.getRelation());
        guarantor.setIdentityDocumentUrl(guarantorDTO.getIdentityDocumentUrl());
        guarantor.setDriver(driver);

        Guarantor saved = guarantorRepository.save(guarantor);

        // Convert to DTO
        GuarantorDTO result = new GuarantorDTO();
        result.setId(saved.getId());
        result.setName(saved.getName());
        result.setPhone(saved.getPhone());
        result.setAddress(saved.getAddress());
        result.setRelation(saved.getRelation());
        result.setIdentityDocumentUrl(saved.getIdentityDocumentUrl());
        result.setDriverId(driver.getId());

        return result;
    }

    /**
     * Request activation - checks if driver has uploaded documents and has 2
     * guarantors
     * Updates status to PENDING_VALIDATION if requirements are met
     * 
     * @param driverId The driver's ID (extracted from JWT)
     */
    @Transactional
    public void requestActivation(Long driverId) {
        // Load driver
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        // Verify user is a driver
        if (driver.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
            throw new IllegalArgumentException("User is not a driver");
        }

        // Check if driver has uploaded identity document
        if (driver.getIdentityDocumentUrl() == null || driver.getIdentityDocumentUrl().trim().isEmpty()) {
            throw new IllegalStateException("Driver must upload identity document before requesting activation");
        }

        // Check if driver has 2 guarantors
        List<Guarantor> guarantors = guarantorRepository.findByDriver(driver);
        if (guarantors.size() < REQUIRED_GUARANTORS) {
            throw new IllegalStateException(
                    String.format("Driver must have %d guarantors. Currently has %d.", REQUIRED_GUARANTORS,
                            guarantors.size()));
        }

        // Verify all guarantors have identity documents
        for (Guarantor guarantor : guarantors) {
            if (guarantor.getIdentityDocumentUrl() == null || guarantor.getIdentityDocumentUrl().trim().isEmpty()) {
                throw new IllegalStateException(
                        String.format("Guarantor '%s' must upload identity document", guarantor.getName()));
            }
        }

        // Update status to PENDING_VALIDATION (if not already)
        if (driver.getStatus() != UserStatus.PENDING_VALIDATION) {
            driver.setStatus(UserStatus.PENDING_VALIDATION);
            utilisateurRepository.save(driver);
        }
    }

    /**
     * Get all guarantors for a driver
     * 
     * @param driverId The driver's ID
     * @return List of guarantor DTOs
     */
    public List<GuarantorDTO> getGuarantors(Long driverId) {
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        return guarantorRepository.findByDriver(driver).stream()
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
    }

    /**
     * Update driver's identity document URL
     * 
     * @param driverId            The driver's ID (extracted from JWT)
     * @param identityDocumentUrl The URL of the uploaded identity document
     */
    @Transactional
    public void updateIdentityDocument(Long driverId, String identityDocumentUrl) {
        // Load driver
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        // Verify user is a driver
        if (driver.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
            throw new IllegalArgumentException("User is not a driver");
        }

        // Validate URL is not empty
        if (identityDocumentUrl == null || identityDocumentUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Identity document URL cannot be empty");
        }

        // Update identity document URL
        driver.setIdentityDocumentUrl(identityDocumentUrl.trim());
        utilisateurRepository.save(driver);
    }

    /**
     * Get driver's own dossier (identity document, guarantors, status, etc.)
     * Minimal response for driver UI
     * 
     * @param driverId The driver's ID (extracted from JWT)
     * @return DriverDossierDTO with driver's information
     */
    public DriverDossierDTO getMyDossier(Long driverId) {
        // Load driver
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        // Verify user is a driver
        if (driver.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
            throw new IllegalArgumentException("User is not a driver");
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

        // Build minimal dossier DTO for driver's own view
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

    /**
     * Update driver's online/offline status
     * 
     * @param driverId The driver's ID (extracted from JWT)
     * @param isOnline Provide true to go online, false to go offline
     */
    @Transactional
    public void updateOnlineStatus(Long driverId, boolean isOnline) {
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        if (driver.getRole() != com.malitrans.transport.model.Role.CHAUFFEUR) {
            throw new IllegalArgumentException("User is not a driver");
        }

        driver.setIsOnline(isOnline);
        utilisateurRepository.save(driver);
    }

    /**
     * Get the driver's current online status
     * 
     * @param driverId The driver's ID
     * @return true if the driver is online
     */
    public boolean getOnlineStatus(Long driverId) {
        Utilisateur driver = utilisateurRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found with ID: " + driverId));

        return driver.getIsOnline() != null && driver.getIsOnline();
    }
}
