package com.malitrans.transport.repository;

import com.malitrans.transport.model.RideRequest;
import com.malitrans.transport.model.ValidationStatus;
import com.malitrans.transport.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RideRequestRepository extends JpaRepository<RideRequest, Long> {
    // Legacy methods (kept for backward compatibility, but deprecated)
    @Deprecated
    List<RideRequest> findByClient(Utilisateur client);
    @Deprecated
    List<RideRequest> findBySupplier(Utilisateur supplier);
    @Deprecated
    List<RideRequest> findByChauffeur(Utilisateur chauffeur);
    @Deprecated
    List<RideRequest> findByValidationStatus(ValidationStatus validationStatus);
    List<RideRequest> findByFlowType(com.malitrans.transport.model.FlowType flowType);
    
    // New methods with descending order by createdAt (LIFO - Last In First Out)
    List<RideRequest> findByClientOrderByCreatedAtDesc(Utilisateur client);
    List<RideRequest> findBySupplierOrderByCreatedAtDesc(Utilisateur supplier);
    List<RideRequest> findByChauffeurOrderByCreatedAtDesc(Utilisateur chauffeur);
    List<RideRequest> findByValidationStatusOrderByCreatedAtDesc(ValidationStatus validationStatus);
    
    /**
     * Find all ride requests for a driver with status COMPLETED or CANCELED,
     * ordered by creation date descending (most recent first)
     * Supports pagination
     */
    @Query("SELECT r FROM RideRequest r WHERE r.chauffeur = :chauffeur " +
           "AND r.validationStatus IN :statuses " +
           "ORDER BY r.createdAt DESC")
    Page<RideRequest> findCompletedOrCanceledByChauffeurOrderByCreatedAtDesc(
            @Param("chauffeur") Utilisateur chauffeur,
            @Param("statuses") List<ValidationStatus> statuses,
            Pageable pageable);
    
    /**
     * Find all active ride requests for a driver (DRIVER_ACCEPTED or IN_TRANSIT)
     * Ordered by creation date descending (most recent first)
     * Supports batching - a driver can have multiple active rides simultaneously
     */
    @Query("SELECT r FROM RideRequest r WHERE r.chauffeur = :chauffeur " +
           "AND r.validationStatus IN :statuses " +
           "ORDER BY r.createdAt DESC")
    List<RideRequest> findActiveByChauffeurOrderByCreatedAtDesc(
            @Param("chauffeur") Utilisateur chauffeur,
            @Param("statuses") List<ValidationStatus> statuses);
}
