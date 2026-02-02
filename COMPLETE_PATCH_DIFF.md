# PATCH COMPLET - CORRECTION SÉCURITÉ 403

## 📦 FICHIERS MODIFIÉS

### 1. `src/main/java/com/malitrans/transport/controller/RideRequestController.java`

**Diff complet :**
```diff
--- a/src/main/java/com/malitrans/transport/controller/RideRequestController.java
+++ b/src/main/java/com/malitrans/transport/controller/RideRequestController.java
@@ -76,7 +76,7 @@ public class RideRequestController {
         @ApiResponse(responseCode = "400", description = "Demande non prête pour la collecte ou chauffeur non actif"),
         @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
     })
-    @PreAuthorize("hasRole('CHAUFFEUR')")
+    @PreAuthorize("hasAuthority('CHAUFFEUR')")
     @PostMapping("/{id}/assign")
     public ResponseEntity<?> assignDriver(@PathVariable Long id) {
 
@@ -109,7 +109,7 @@ public class RideRequestController {
         @ApiResponse(responseCode = "400", description = "Demande ne peut pas être validée dans son état actuel"),
         @ApiResponse(responseCode = "403", description = "Accès refusé - vous n'êtes pas autorisé à valider cette demande")
     })
-    @PreAuthorize("hasAnyRole('CLIENT', 'SUPPLIER')")
+    @PreAuthorize("hasAnyAuthority('CLIENT', 'SUPPLIER')")
     @PostMapping("/{id}/validate")
     public ResponseEntity<?> validateRequest(@PathVariable Long id) {
 
@@ -134,7 +134,7 @@ public class RideRequestController {
         @ApiResponse(responseCode = "400", description = "Code incorrect, statut invalide ou chauffeur non assigné"),
         @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
     })
-    @PreAuthorize("hasRole('CHAUFFEUR')")
+    @PreAuthorize("hasAuthority('CHAUFFEUR')")
     @PostMapping("/{id}/pickup")
     public ResponseEntity<?> validatePickup(@PathVariable Long id, @RequestBody ValidateCodeDTO request) {
 
@@ -165,7 +165,7 @@ public class RideRequestController {
         @ApiResponse(responseCode = "400", description = "Code incorrect, statut invalide ou chauffeur non assigné"),
         @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
     })
-    @PreAuthorize("hasRole('CHAUFFEUR')")
+    @PreAuthorize("hasAuthority('CHAUFFEUR')")
     @PostMapping("/{id}/delivery")
     public ResponseEntity<?> validateDelivery(@PathVariable Long id, @RequestBody ValidateCodeDTO request) {
 
@@ -198,7 +198,7 @@ public class RideRequestController {
         @ApiResponse(responseCode = "400", description = "QR code invalide ou chauffeur non assigné"),
         @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
     })
-    @PreAuthorize("hasRole('CHAUFFEUR')")
+    @PreAuthorize("hasAuthority('CHAUFFEUR')")
     @PostMapping("/{id}/scan-qr")
     public ResponseEntity<?> scanQrCode(
 
@@ -226,7 +226,7 @@ public class RideRequestController {
                            "clientId est automatiquement extrait du JWT.")
     @ApiResponses({@ApiResponse(responseCode = "200")})
-    @PreAuthorize("hasRole('CLIENT')")
+    @PreAuthorize("hasAuthority('CLIENT')")
     @GetMapping("/client/history")
     public List<RideRequestDTO> historyClient() {
 
@@ -238,7 +238,7 @@ public class RideRequestController {
                            "supplierId est automatiquement extrait du JWT.")
     @ApiResponses({@ApiResponse(responseCode = "200")})
-    @PreAuthorize("hasRole('SUPPLIER')")
+    @PreAuthorize("hasAuthority('SUPPLIER')")
     @GetMapping("/supplier/history")
     public List<RideRequestDTO> historySupplier() {
 
@@ -256,7 +256,7 @@ public class RideRequestController {
         @ApiResponse(responseCode = "200", description = "Liste des courses actives (peut être vide)"),
         @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
     })
-    @PreAuthorize("hasRole('CHAUFFEUR')")
+    @PreAuthorize("hasAuthority('CHAUFFEUR')")
     @GetMapping("/chauffeur/active")
     public ResponseEntity<List<RideRequestDTO>> getActiveRides() {
 
@@ -274,7 +274,7 @@ public class RideRequestController {
         @ApiResponse(responseCode = "200", description = "Historique récupéré avec succès"),
         @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un chauffeur")
     })
-    @PreAuthorize("hasRole('CHAUFFEUR')")
+    @PreAuthorize("hasAuthority('CHAUFFEUR')")
     @GetMapping("/chauffeur/history")
     public ResponseEntity<PaginatedResponse<RideRequestDTO>> historyChauffeur(
```

---

### 2. `src/main/java/com/malitrans/transport/controller/AdminController.java`

**Diff complet :**
```diff
--- a/src/main/java/com/malitrans/transport/controller/AdminController.java
+++ b/src/main/java/com/malitrans/transport/controller/AdminController.java
@@ -15,7 +15,7 @@ import java.util.Map;
 
 @RestController
 @RequestMapping("/admin")
-@PreAuthorize("hasRole('ADMIN')")
+@PreAuthorize("hasAuthority('ADMIN')")
 public class AdminController {
```

---

### 3. `src/main/java/com/malitrans/transport/controller/DriverController.java`

**Diff complet :**
```diff
--- a/src/main/java/com/malitrans/transport/controller/DriverController.java
+++ b/src/main/java/com/malitrans/transport/controller/DriverController.java
@@ -16,7 +16,7 @@ import java.util.Map;
 
 @RestController
 @RequestMapping("/driver")
-@PreAuthorize("hasRole('CHAUFFEUR')")
+@PreAuthorize("hasAuthority('CHAUFFEUR')")
 public class DriverController {
```

---

### 4. `src/main/java/com/malitrans/transport/controller/UserController.java`

**Diff complet :**
```diff
--- a/src/main/java/com/malitrans/transport/controller/UserController.java
+++ b/src/main/java/com/malitrans/transport/controller/UserController.java
@@ -44,7 +44,7 @@ public class UserController {
         @ApiResponse(responseCode = "403", description = "Accès refusé - doit être un client")
     })
     @GetMapping("/suppliers")
-    @PreAuthorize("hasRole('CLIENT')")
+    @PreAuthorize("hasAuthority('CLIENT')")
     public ResponseEntity<List<UtilisateurDTO>> getSuppliers() {
```

---

### 5. `pom.xml`

**Diff complet :**
```diff
--- a/pom.xml
+++ b/pom.xml
@@ -77,6 +77,11 @@
       <groupId>org.springdoc</groupId>
       <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
       <version>2.3.0</version>
     </dependency>
+    <!-- Spring Boot Test for MockMvc security tests -->
+    <dependency>
+      <groupId>org.springframework.boot</groupId>
+      <artifactId>spring-boot-starter-test</artifactId>
+      <scope>test</scope>
+    </dependency>
   </dependencies>
```

---

## 📁 FICHIERS NOUVEAUX CRÉÉS

### 1. `src/main/java/com/malitrans/transport/dto/ErrorResponse.java`

**Contenu complet :**
```java
package com.malitrans.transport.dto;

import java.time.LocalDateTime;

public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
```

---

### 2. `src/main/java/com/malitrans/transport/exception/GlobalExceptionHandler.java`

**Contenu complet :**
```java
package com.malitrans.transport.exception;

import com.malitrans.transport.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                ex.getMessage() != null ? ex.getMessage() : "Access denied",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage() != null ? ex.getMessage() : "Authentication failed",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Invalid credentials",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

### 3. `src/test/java/com/malitrans/transport/controller/SecurityTest.java`

**Contenu complet :** (voir fichier créé précédemment)

---

## ✅ CHECKLIST POSTMAN

### Prérequis
1. ✅ Démarrer l'application Spring Boot
2. ✅ Créer un utilisateur CHAUFFEUR via `POST /api/auth/register`
3. ✅ Créer un utilisateur SUPPLIER via `POST /api/auth/register`
4. ✅ Obtenir les tokens JWT pour chaque utilisateur

### Tests à exécuter

#### ✅ Test 1 : CHAUFFEUR → 200 OK sur `/api/ride/chauffeur/active`
```
GET http://localhost:8080/api/ride/chauffeur/active
Authorization: Bearer <TOKEN_CHAUFFEUR>
```
**Attendu :** Status 200, Body = `[]` ou liste de courses

#### ✅ Test 2 : CHAUFFEUR → 200 OK sur `/api/ride/chauffeur/history`
```
GET http://localhost:8080/api/ride/chauffeur/history?page=1&limit=20
Authorization: Bearer <TOKEN_CHAUFFEUR>
```
**Attendu :** Status 200, Body = `{data: [], meta: {...}}`

#### ✅ Test 3 : SUPPLIER → 403 Forbidden sur `/api/ride/chauffeur/active`
```
GET http://localhost:8080/api/ride/chauffeur/active
Authorization: Bearer <TOKEN_SUPPLIER>
```
**Attendu :** Status 403, Body = `{status: 403, error: "Forbidden", ...}`

#### ✅ Test 4 : SUPPLIER → 403 Forbidden sur `/api/ride/chauffeur/history`
```
GET http://localhost:8080/api/ride/chauffeur/history?page=1&limit=20
Authorization: Bearer <TOKEN_SUPPLIER>
```
**Attendu :** Status 403, Body = `{status: 403, error: "Forbidden", ...}`

#### ✅ Test 5 : Sans token → 401 Unauthorized sur `/api/ride/chauffeur/active`
```
GET http://localhost:8080/api/ride/chauffeur/active
(no Authorization header)
```
**Attendu :** Status 401, Body = `{status: 401, error: "Unauthorized", ...}`

#### ✅ Test 6 : Sans token → 401 Unauthorized sur `/api/ride/chauffeur/history`
```
GET http://localhost:8080/api/ride/chauffeur/history?page=1&limit=20
(no Authorization header)
```
**Attendu :** Status 401, Body = `{status: 401, error: "Unauthorized", ...}`

---

## 📊 RÉSUMÉ

**Fichiers modifiés :** 5
- RideRequestController.java (9 changements)
- AdminController.java (1 changement)
- DriverController.java (1 changement)
- UserController.java (1 changement)
- pom.xml (ajout dépendance test)

**Fichiers créés :** 3
- ErrorResponse.java
- GlobalExceptionHandler.java
- SecurityTest.java

**Total :** 8 fichiers touchés

---

**✅ Patch prêt à être appliqué**

