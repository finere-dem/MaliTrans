package com.malitrans.transport.repository;

import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.UserStatus;
import com.malitrans.transport.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    Optional<Utilisateur> findByUsername(String username);
    Optional<Utilisateur> findByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByPhone(String phone);
    List<Utilisateur> findByRole(Role role);

    /**
     * Find all drivers (CHAUFFEUR) for a specific company with optional filters using native SQL
     * Uses ILIKE for PostgreSQL case-insensitive search (avoids LOWER() on bytea error)
     * @param role Role as String (e.g., "CHAUFFEUR")
     * @param companyId The company ID
     * @param status Optional status filter as String (null to ignore)
     * @param searchQuery Optional search query for username, phone, or matricule (null or empty to ignore)
     * @param pageable Pagination parameters
     * @return Page of drivers
     */
    @Query(value = "SELECT u.* FROM utilisateur u WHERE " +
           "u.role = :role AND " +
           "u.company_id = :companyId AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:searchQuery IS NULL OR :searchQuery = '' OR " +
           "  u.username ILIKE '%' || :searchQuery || '%' OR " +
           "  u.phone ILIKE '%' || :searchQuery || '%' OR " +
           "  u.matricule ILIKE '%' || :searchQuery || '%') " +
           "ORDER BY u.id DESC",
           countQuery = "SELECT COUNT(*) FROM utilisateur u WHERE " +
           "u.role = :role AND " +
           "u.company_id = :companyId AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:searchQuery IS NULL OR :searchQuery = '' OR " +
           "  u.username ILIKE '%' || :searchQuery || '%' OR " +
           "  u.phone ILIKE '%' || :searchQuery || '%' OR " +
           "  u.matricule ILIKE '%' || :searchQuery || '%')",
           nativeQuery = true)
    Page<Utilisateur> findDriversByCompanyNative(
            @Param("role") String role,
            @Param("companyId") Long companyId,
            @Param("status") String status,
            @Param("searchQuery") String searchQuery,
            Pageable pageable
    );

    /**
     * Find all drivers pending company verification for a specific company
     * Efficient JPQL query to replace findAll().stream() usage
     * @param role The driver role (CHAUFFEUR)
     * @param companyId The company ID
     * @param status The status filter (PENDING_COMPANY_VERIFICATION)
     * @return List of drivers ordered by id DESC
     */
    @Query("SELECT u FROM Utilisateur u WHERE " +
           "u.role = :role AND " +
           "u.company.id = :companyId AND " +
           "u.status = :status " +
           "ORDER BY u.id DESC")
    List<Utilisateur> findDriversPendingForCompany(
            @Param("role") Role role,
            @Param("companyId") Long companyId,
            @Param("status") UserStatus status
    );
}
