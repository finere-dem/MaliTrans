package com.malitrans.transport.service;

import com.malitrans.transport.dto.DriverValidationDTO;
import com.malitrans.transport.dto.GuarantorDTO;
import com.malitrans.transport.model.Guarantor;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.model.UserStatus;
import com.malitrans.transport.repository.GuarantorRepository;
import com.malitrans.transport.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UtilisateurRepository utilisateurRepository;
    private final GuarantorRepository guarantorRepository;

    public AdminService(UtilisateurRepository utilisateurRepository, GuarantorRepository guarantorRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.guarantorRepository = guarantorRepository;
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

