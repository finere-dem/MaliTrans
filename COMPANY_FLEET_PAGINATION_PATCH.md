# PATCH - PAGINATION FLOTTE COMPAGNIE

## 📋 RÉSUMÉ

**Objectif** : Ajouter une pagination professionnelle pour la flotte de compagnie, similaire à `GET /api/ride/chauffeur/history`

**Endpoint créé** : `GET /api/company/drivers?page=1&limit=20&status=&q=`

**Sécurité** : `@PreAuthorize("hasAnyAuthority('COMPANY_MANAGER','SUPPLIER')")`

---

## 📁 FICHIERS CRÉÉS

### 1. `src/main/java/com/malitrans/transport/dto/DriverSummaryDTO.java`

**Contenu complet :**
```java
package com.malitrans.transport.dto;

import com.malitrans.transport.model.UserStatus;
import java.time.LocalDateTime;

public class DriverSummaryDTO {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private UserStatus status;
    private String matricule;
    private LocalDateTime createdAt;
    // ... getters/setters
}
```

**Champs :**
- `id` : ID du chauffeur
- `username` : Nom d'utilisateur
- `fullName` : Nom complet (firstName + lastName ou username)
- `phone` : Numéro de téléphone
- `status` : Statut (UserStatus enum)
- `matricule` : Code interne unique
- `createdAt` : Date de création (null si non disponible)

---

### 2. `src/test/java/com/malitrans/transport/controller/CompanyControllerSecurityTest.java`

**Tests MockMvc implémentés :**
1. ✅ `testCompanyManagerCanAccessFleetList()` - COMPANY_MANAGER → 200 OK
2. ✅ `testSupplierCanAccessFleetList()` - SUPPLIER → 200 OK
3. ✅ `testChauffeurCannotAccessFleetList()` - CHAUFFEUR → 403 Forbidden
4. ✅ `testUnauthenticatedUserCannotAccessFleetList()` - Sans token → 401 Unauthorized
5. ✅ `testFleetListWithFilters()` - Test avec filtres status et q
6. ✅ `testFleetListPagination()` - Test pagination page 2, limit 10

---

## 🔧 FICHIERS MODIFIÉS

### 1. `src/main/java/com/malitrans/transport/dto/PaginatedResponse.java`

**Ajout du champ `pageSize` dans Meta :**

```diff
    public static class Meta {
        private long totalItems;
        private int currentPage;
        private int totalPages;
+       private int pageSize;

        public Meta() {
        }

+       public Meta(long totalItems, int currentPage, int totalPages, int pageSize) {
+           this.totalItems = totalItems;
+           this.currentPage = currentPage;
+           this.totalPages = totalPages;
+           this.pageSize = pageSize;
+       }

        // Legacy constructor for backward compatibility
        public Meta(long totalItems, int currentPage, int totalPages) {
            this.totalItems = totalItems;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
        }

+       public int getPageSize() {
+           return pageSize;
+       }
+
+       public void setPageSize(int pageSize) {
+           this.pageSize = pageSize;
+       }
    }
```

---

### 2. `src/main/java/com/malitrans/transport/repository/UtilisateurRepository.java`

**Ajout de la méthode de pagination avec filtres :**

```diff
+import org.springframework.data.domain.Page;
+import org.springframework.data.domain.Pageable;
+import org.springframework.data.jpa.repository.Query;
+import org.springframework.data.repository.query.Param;
+import com.malitrans.transport.model.UserStatus;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {
    // ... méthodes existantes

+   /**
+    * Find all drivers (CHAUFFEUR) for a specific company with optional filters
+    */
+   @Query("SELECT u FROM Utilisateur u WHERE " +
+          "u.role = :role AND " +
+          "u.company.id = :companyId AND " +
+          "(:status IS NULL OR u.status = :status) AND " +
+          "(:searchQuery IS NULL OR " +
+          "  LOWER(u.username) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
+          "  LOWER(u.phone) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR " +
+          "  LOWER(u.matricule) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
+          "ORDER BY u.id DESC")
+   Page<Utilisateur> findDriversByCompany(
+           @Param("role") Role role,
+           @Param("companyId") Long companyId,
+           @Param("status") UserStatus status,
+           @Param("searchQuery") String searchQuery,
+           Pageable pageable
+   );
}
```

**Fonctionnalités :**
- Filtre par `companyId` (obligatoire)
- Filtre optionnel par `status` (UserStatus)
- Recherche optionnelle `q` dans username, phone, matricule (case-insensitive)
- Tri par `id DESC` (plus récents en premier)
- Pagination via `Pageable`

---

### 3. `src/main/java/com/malitrans/transport/service/CompanyService.java`

**Ajout des imports :**
```diff
+import com.malitrans.transport.dto.DriverSummaryDTO;
+import com.malitrans.transport.dto.PaginatedResponse;
+import org.springframework.data.domain.Page;
+import org.springframework.data.domain.PageRequest;
+import org.springframework.data.domain.Pageable;
+import java.time.LocalDateTime;
```

**Ajout de la méthode `getCompanyDrivers` :**

```java
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

    // Create Pageable (Spring Data uses 0-based page index)
    Pageable pageable = PageRequest.of(page - 1, limit);

    // Query drivers with pagination and filters
    Page<Utilisateur> pageResult = utilisateurRepository.findDriversByCompany(
            Role.CHAUFFEUR,
            companyId,
            status,
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
                dto.setCreatedAt(null); // Utilisateur doesn't have createdAt field
                return dto;
            })
            .collect(Collectors.toList());

    // Create meta object with pageSize
    PaginatedResponse.Meta meta = new PaginatedResponse.Meta(
            pageResult.getTotalElements(),
            page,
            pageResult.getTotalPages(),
            limit
    );

    return new PaginatedResponse<>(dtos, meta);
}
```

**Fonctionnalités :**
- Validation des paramètres (page ≥ 1, limit entre 1 et 100)
- Vérification du rôle (COMPANY_MANAGER ou SUPPLIER)
- Vérification que le manager appartient à une compagnie
- Conversion des entités en DTOs
- Retourne `PaginatedResponse<DriverSummaryDTO>` avec métadonnées complètes

---

### 4. `src/main/java/com/malitrans/transport/controller/CompanyController.java`

**Ajout des imports :**
```diff
+import com.malitrans.transport.dto.DriverSummaryDTO;
+import com.malitrans.transport.dto.PaginatedResponse;
+import com.malitrans.transport.model.UserStatus;
+import io.swagger.v3.oas.annotations.Parameter;
```

**Ajout de l'endpoint `GET /company/drivers` :**

```java
@Operation(summary = "Lister la flotte de chauffeurs (paginé)", 
           description = "Retourne la liste paginée de tous les chauffeurs de l'entreprise du manager. " +
                       "Supporte le filtrage par statut et la recherche par username, téléphone ou matricule. " +
                       "managerId est automatiquement extrait du JWT.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Liste paginée des chauffeurs récupérée avec succès"),
    @ApiResponse(responseCode = "400", description = "Paramètres invalides"),
    @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un company manager ou supplier"),
    @ApiResponse(responseCode = "401", description = "Non authentifié")
})
@GetMapping("/drivers")
public ResponseEntity<PaginatedResponse<DriverSummaryDTO>> getCompanyDrivers(
        @Parameter(description = "Numéro de page (défaut: 1)", example = "1")
        @RequestParam(defaultValue = "1") int page,
        @Parameter(description = "Nombre d'éléments par page (défaut: 20, max: 100)", example = "20")
        @RequestParam(defaultValue = "20") int limit,
        @Parameter(description = "Filtre optionnel par statut (PENDING_COMPANY_VERIFICATION, PENDING_ADMIN_APPROVAL, ACTIVE, etc.)", required = false)
        @RequestParam(required = false) String status,
        @Parameter(description = "Recherche optionnelle dans username, téléphone ou matricule", required = false)
        @RequestParam(required = false) String q) {
    
    Long managerId = SecurityUtil.getCurrentUserId();
    
    // Parse status if provided
    UserStatus statusEnum = null;
    if (status != null && !status.trim().isEmpty()) {
        try {
            statusEnum = UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Invalid status, will be ignored (statusEnum remains null)
        }
    }
    
    PaginatedResponse<DriverSummaryDTO> response = companyService.getCompanyDrivers(
            managerId, page, limit, statusEnum, q);
    
    return ResponseEntity.ok(response);
}
```

**Fonctionnalités :**
- Paramètres de pagination : `page` (défaut: 1), `limit` (défaut: 20, max: 100)
- Filtre optionnel `status` (string → UserStatus enum)
- Recherche optionnelle `q` (username, phone, matricule)
- `managerId` extrait automatiquement du JWT via `SecurityUtil.getCurrentUserId()`
- Annotations Swagger complètes

---

### 5. `src/main/java/com/malitrans/transport/service/RideRequestService.java`

**Mise à jour pour inclure `pageSize` dans Meta :**

```diff
-            new PaginatedResponse.Meta(0, page, 0)
+            new PaginatedResponse.Meta(0, page, 0, limit)

-            new PaginatedResponse.Meta(
-                    pageResult.getTotalElements(),
-                    page,
-                    totalPages
-            )
+            new PaginatedResponse.Meta(
+                    pageResult.getTotalElements(),
+                    page,
+                    totalPages,
+                    limit
+            )

-        PaginatedResponse.Meta meta = new PaginatedResponse.Meta(
-                pageResult.getTotalElements(),
-                page,
-                totalPages
-        );
+        PaginatedResponse.Meta meta = new PaginatedResponse.Meta(
+                pageResult.getTotalElements(),
+                page,
+                totalPages,
+                limit
+        );
```

**Raison :** Cohérence avec le nouveau format de `PaginatedResponse.Meta` incluant `pageSize`

---

## 📊 STRUCTURE DE LA RÉPONSE JSON

### Exemple de réponse réussie (200 OK) :

```json
{
  "data": [
    {
      "id": 1,
      "username": "driver1",
      "fullName": "John Doe",
      "phone": "+22370123456",
      "status": "ACTIVE",
      "matricule": "COM-2024-0001",
      "createdAt": null
    },
    {
      "id": 2,
      "username": "driver2",
      "fullName": "Jane Smith",
      "phone": "+22370987654",
      "status": "PENDING_ADMIN_APPROVAL",
      "matricule": "COM-2024-0002",
      "createdAt": null
    }
  ],
  "meta": {
    "totalItems": 25,
    "currentPage": 1,
    "totalPages": 2,
    "pageSize": 20
  }
}
```

---

## ✅ CHECKLIST POSTMAN

### Prérequis
1. ✅ Démarrer l'application Spring Boot
2. ✅ Créer un utilisateur COMPANY_MANAGER via `POST /api/auth/register`
3. ✅ Créer un utilisateur SUPPLIER via `POST /api/auth/register`
4. ✅ Créer un utilisateur CHAUFFEUR via `POST /api/auth/register`
5. ✅ Obtenir les tokens JWT pour chaque utilisateur

### Tests à exécuter

#### ✅ Test 1 : COMPANY_MANAGER → 200 OK sur `/api/company/drivers`
```
GET http://localhost:8080/api/company/drivers?page=1&limit=20
Authorization: Bearer <TOKEN_COMPANY_MANAGER>
```
**Attendu :** Status 200, Body = `{data: [...], meta: {...}}`

#### ✅ Test 2 : SUPPLIER → 200 OK sur `/api/company/drivers`
```
GET http://localhost:8080/api/company/drivers?page=1&limit=20
Authorization: Bearer <TOKEN_SUPPLIER>
```
**Attendu :** Status 200, Body = `{data: [...], meta: {...}}`

#### ✅ Test 3 : CHAUFFEUR → 403 Forbidden sur `/api/company/drivers`
```
GET http://localhost:8080/api/company/drivers?page=1&limit=20
Authorization: Bearer <TOKEN_CHAUFFEUR>
```
**Attendu :** Status 403, Body = `{status: 403, error: "Forbidden", ...}`

#### ✅ Test 4 : Sans token → 401 Unauthorized sur `/api/company/drivers`
```
GET http://localhost:8080/api/company/drivers?page=1&limit=20
(no Authorization header)
```
**Attendu :** Status 401, Body = `{status: 401, error: "Unauthorized", ...}`

#### ✅ Test 5 : Filtre par statut
```
GET http://localhost:8080/api/company/drivers?page=1&limit=20&status=ACTIVE
Authorization: Bearer <TOKEN_COMPANY_MANAGER>
```
**Attendu :** Status 200, Seuls les chauffeurs avec status=ACTIVE

#### ✅ Test 6 : Recherche par query
```
GET http://localhost:8080/api/company/drivers?page=1&limit=20&q=driver1
Authorization: Bearer <TOKEN_COMPANY_MANAGER>
```
**Attendu :** Status 200, Résultats contenant "driver1" dans username, phone ou matricule

#### ✅ Test 7 : Pagination page 2
```
GET http://localhost:8080/api/company/drivers?page=2&limit=10
Authorization: Bearer <TOKEN_COMPANY_MANAGER>
```
**Attendu :** Status 200, `meta.currentPage = 2`, `meta.pageSize = 10`

#### ✅ Test 8 : Isolation des compagnies
**Scénario :** Manager1 (Company1) ne doit pas voir les drivers de Company2
```
GET http://localhost:8080/api/company/drivers?page=1&limit=20
Authorization: Bearer <TOKEN_MANAGER1>
```
**Attendu :** Status 200, Seuls les drivers de Company1

---

## 🔒 SÉCURITÉ

### Vérifications implémentées :
1. ✅ `@PreAuthorize("hasAnyAuthority('COMPANY_MANAGER','SUPPLIER')")` au niveau du controller
2. ✅ Vérification du rôle dans `CompanyService.getCompanyDrivers()`
3. ✅ Isolation par compagnie : Un manager ne voit que les drivers de sa compagnie
4. ✅ `managerId` extrait du JWT (Zero Trust)

### Endpoints protégés :
- `GET /api/company/drivers` : Requiert COMPANY_MANAGER ou SUPPLIER
- `GET /api/company/drivers/pending` : Requiert COMPANY_MANAGER ou SUPPLIER (existant)
- `POST /api/company/drivers/{driverId}/validate` : Requiert COMPANY_MANAGER ou SUPPLIER (existant)

---

## 📈 PERFORMANCE

### Optimisations :
- ✅ Pagination au niveau base de données (pas de chargement complet en mémoire)
- ✅ Requête JPA optimisée avec `@Query`
- ✅ Limite maximale de 100 éléments par page
- ✅ Index recommandé sur `company_id`, `role`, `status` (à ajouter en BDD si nécessaire)

---

## 🎯 RÉSUMÉ DES MODIFICATIONS

| Fichier | Type | Description |
|---------|------|-------------|
| `DriverSummaryDTO.java` | **NOUVEAU** | DTO pour résumé de chauffeur |
| `CompanyControllerSecurityTest.java` | **NOUVEAU** | Tests MockMvc de sécurité |
| `PaginatedResponse.java` | Modification | Ajout champ `pageSize` dans Meta |
| `UtilisateurRepository.java` | Modification | Ajout méthode `findDriversByCompany` |
| `CompanyService.java` | Modification | Ajout méthode `getCompanyDrivers` |
| `CompanyController.java` | Modification | Ajout endpoint `GET /company/drivers` |
| `RideRequestService.java` | Modification | Mise à jour Meta avec `pageSize` |

**Total :** 2 fichiers nouveaux, 5 fichiers modifiés

---

## ✅ VALIDATION

### Compilation
```bash
mvn clean compile
```
✅ Vérifier qu'il n'y a pas d'erreurs de compilation

### Tests
```bash
mvn test -Dtest=CompanyControllerSecurityTest
```
✅ Vérifier que tous les tests passent

### Démarrage
```bash
mvn spring-boot:run
```
✅ Vérifier que l'application démarre sans erreur

---

**✅ Patch prêt à être appliqué**

