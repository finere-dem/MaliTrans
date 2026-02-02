# Récapitulatif sécurité MaliTrans — Existant et intégration OTP

## 1. Ce qui est déjà implémenté

### 1.1 Authentification (Auth)

| Élément | Détail |
|--------|--------|
| **Inscription** | `POST /auth/register` — Création compte (username, password, rôle, phone, companyId pour CHAUFFEUR). Mot de passe hashé BCrypt. Rôles acceptés : CLIENT, CHAUFFEUR (DRIVER), etc. |
| **Connexion** | `POST /auth/login` — Login par **username + password**. Retourne JWT + refresh token + username, role, userId. |
| **Refresh token** | `POST /auth/refresh-token` — Renouvellement de l’access token à partir d’un refresh token valide (UUID, durée configurable, ex. 7 jours). |

**Fichiers :** `AuthController`, `AuthService` / `AuthServiceImpl`, `RegisterDTO`, `LoginRequest`, `RefreshTokenRequest`, `AuthResponse`.

---

### 1.2 JWT (Jetons d’accès)

| Élément | Détail |
|--------|--------|
| **Génération** | `JwtTokenUtil.generateToken(username, roles)` — Token signé HS512, avec claim `roles` (liste de rôles). |
| **Validation** | Vérification signature + expiration. Extraction du username (subject) et des rôles depuis le token. |
| **Config** | `jwt.secret`, `jwt.expiration` (ex. 1h), `jwt.refreshExpiration` (ex. 7 jours) dans `application.properties`. |

**Fichiers :** `JwtTokenUtil`, `JwtAuthenticationFilter`.

---

### 1.3 Filtre d’authentification JWT

| Élément | Détail |
|--------|--------|
| **Filtre** | `JwtAuthenticationFilter` (avant `UsernamePasswordAuthenticationFilter`). |
| **Comportement** | Lit `Authorization: Bearer <token>`, valide le JWT, charge les rôles depuis le token, vérifie que l’utilisateur existe et est `enabled` via `UserDetailsService`, puis pose dans le `SecurityContext` une `UsernamePasswordAuthenticationToken` avec **principal = String (username)** et les authorities = rôles. |

**Important pour OTP :** Le principal reste le **username** (String), utilisé par `SecurityUtil.getCurrentUsername()` / `getCurrentUserId()`.

---

### 1.4 Rôles et autorisations

| Rôle | Usage |
|------|--------|
| **CLIENT** | Création de demandes, historique client, notation. |
| **CHAUFFEUR** | Courses actives, historique chauffeur, assignation, validation pickup/delivery (code/QR). |
| **SUPPLIER** | Accès flotte `/company/drivers`, validation chauffeurs, dossiers. |
| **COMPANY_MANAGER** | Idem + validation chauffeurs, pending drivers. |
| **ADMIN** | Endpoints admin (utilisateurs, validation globale). |

**Mécanismes :**  
- `SecurityConfig` : `requestMatchers` par chemin (ex. `/auth/**` en `permitAll()`, `/company/**` en `hasAnyAuthority("COMPANY_MANAGER","SUPPLIER")`).  
- `@PreAuthorize("hasAnyAuthority('...')")` sur les contrôleurs (Company, RideRequest, Driver, Admin, etc.).  
- Rôles portés dans le JWT (pas de préfixe `ROLE_`), cohérents avec `CustomUserDetailsService` et `SecurityUtil`.

---

### 1.5 Utilitaire “utilisateur courant”

| Méthode | Rôle |
|--------|------|
| `SecurityUtil.getCurrentUsername()` | Retourne le username (principal JWT). Lance si non authentifié ou principal non-String. |
| `SecurityUtil.getCurrentUser()` | Charge l’entité `Utilisateur` depuis la DB via le username. |
| `SecurityUtil.getCurrentUserId()` | Retourne l’id de l’utilisateur courant. |

**Fichiers :** `SecurityUtil`, `CustomUserDetailsService`, `UtilisateurRepository`.

---

### 1.6 Refresh tokens

| Élément | Détail |
|--------|--------|
| **Stockage** | Entité `RefreshToken` (user, token UUID, expiryDate). Un token actif par utilisateur (les anciens sont supprimés à la création). |
| **Service** | `RefreshTokenService` : création, vérification expiration, suppression par userId ou par token. |
| **Exception** | `TokenRefreshException` si token invalide ou expiré. |

**Fichiers :** `RefreshToken`, `RefreshTokenRepository`, `RefreshTokenService`.

---

### 1.7 Sécurité HTTP et CORS

| Élément | Détail |
|--------|--------|
| **CSRF** | Désactivé (API stateless JWT). |
| **Sessions** | Stateless (`SessionCreationPolicy.STATELESS`). |
| **CORS** | Configuré (origines, méthodes, en-têtes) via `CorsConfigurationSource`. |

---

### 1.8 Gestion des erreurs (sécurité)

| Exception | Réponse |
|-----------|----------|
| `AccessDeniedException` | 403 + `ErrorResponse` (status, error, message, path). |
| `AuthenticationException` / `BadCredentialsException` | 401 + `ErrorResponse`. |
| `IllegalStateException` / `IllegalArgumentException` | 400 selon le handler. |

**Fichier :** `GlobalExceptionHandler`.

---

### 1.9 Modèle utilisateur (pour OTP)

- **Utilisateur :** `username`, `password` (BCrypt), `phone`, `role`, `enabled`, `status` (UserStatus), etc.  
- **Pas de champ OTP/code de vérification** actuellement (ni `email` explicite si tout passe par `username`/`phone`).  
- Le **téléphone** (`phone`) est déjà présent : idéal pour envoi SMS OTP.

---

## 2. Où et comment ajouter l’OTP

L’OTP (code à usage unique) peut servir à plusieurs usages. Voici les points d’intégration dans l’existant.

### 2.1 Cas d’usage possibles

1. **Vérification à l’inscription** — Après `register`, exiger la saisie d’un code OTP (envoyé par SMS ou email) avant de considérer le compte actif.
2. **Double facteur à la connexion (2FA)** — Après login username/password réussi, exiger un second facteur (OTP SMS ou app) avant de délivrer JWT + refresh token.
3. **Réinitialisation du mot de passe** — Demande de reset (par username/phone/email) → envoi OTP → vérification OTP + nouveau mot de passe.
4. **Actions sensibles** — Ex. validation chauffeur, changement de numéro de téléphone : exiger un OTP envoyé au numéro concerné.

### 2.2 Points d’intégration dans le code actuel

| Objectif | Où intervenir | Remarques |
|----------|----------------|-----------|
| **OTP à l’inscription** | `AuthServiceImpl.register()` + nouvel endpoint `POST /auth/verify-registration` | Après `save()`, ne pas activer le compte (ou mettre un statut “pending_verification”) ; envoyer OTP (SMS/email) ; nouvel endpoint reçoit `username` + `code`, vérifie OTP puis active le compte (et éventuellement connecte avec JWT). |
| **OTP à la connexion (2FA)** | `AuthServiceImpl.login()` + nouvel endpoint `POST /auth/verify-login-otp` | Après `authenticationManager.authenticate()` réussi, ne pas renvoyer tout de suite `AuthResponse`. Stocker temporairement “login en attente OTP” (cache/session ou token temporaire) et renvoyer `{ "requireOtp": true, "tempToken": "..." }`. Le client envoie OTP sur `verify-login-otp` ; si OK, générer JWT + refresh token comme aujourd’hui. |
| **Reset password** | Nouveaux endpoints : `POST /auth/forgot-password`, `POST /auth/reset-password` | `forgot-password` : identifier l’utilisateur (username/phone), générer OTP, l’envoyer par SMS/email, stocker hash + expiry. `reset-password` : recevoir OTP + nouveau mot de passe, vérifier OTP puis mettre à jour le mot de passe avec `PasswordEncoder`. |

### 2.3 Composants à ajouter (côté backend)

- **Modèle / stockage OTP**  
  - Soit entité dédiée (ex. `OtpCode` : userId ou username, code hashé ou chiffré, type [REGISTRATION, LOGIN_2FA, PASSWORD_RESET], expiration).  
  - Soit cache (ex. Redis, Caffeine) avec clé = username/phone + type, valeur = code (ou hash) + expiry.

- **Envoi du code**  
  - SMS : intégration fournisseur (Twilio, AWS SNS, etc.) avec le `phone` déjà présent sur `Utilisateur`.  
  - Email : ajouter un champ `email` sur `Utilisateur` si besoin, puis recourir à un service d’envoi (Spring Mail, SendGrid, etc.).

- **Configuration**  
  - Taille du code (6 chiffres), durée de validité (ex. 5–10 min), limite de tentatives (anti-bruteforce).  
  - Propriétés dans `application.properties` (ex. `otp.expiration-minutes`, `otp.length`).

- **Sécurité**  
  - Ne pas logger le code en clair.  
  - Limiter le nombre de demandes OTP par utilisateur / IP (rate limiting).  
  - Après X échecs de vérification, invalider le code et éventuellement bloquer temporairement.

### 2.4 Endpoints à exposer (recommandation)

- `POST /auth/send-otp` — Corps : `{ "username" ou "phone", "purpose": "REGISTRATION" | "LOGIN_2FA" | "PASSWORD_RESET" }`. Génère OTP, l’envoie (SMS/email), le stocke avec expiration. Réponse : `{ "message": "OTP sent" }` (sans révéler la durée exacte d’expiration si besoin).
- `POST /auth/verify-otp` — Corps : `{ "username" ou "phone", "code", "purpose" }`. Vérifie le code ; selon le `purpose`, active le compte, complète le login 2FA, ou autorise l’étape suivante (ex. reset password).
- Pour le 2FA au login : soit `login` retourne `requireOtp` + `tempToken`, soit un seul endpoint `POST /auth/verify-login-otp` avec `tempToken` + `code` qui renvoie `AuthResponse` en cas de succès.

Selon le choix (inscription, 2FA login, reset password), on branche ces endpoints sur les bons flux existants (register, login, et nouveaux flux reset).

---

## 3. Résumé

| Bloc | État actuel |
|------|-------------|
| Inscription / Login / Refresh | Implémenté (username + password, JWT + refresh token). |
| JWT (claims, rôles, validation) | Implémenté. |
| Filtre JWT + SecurityContext (principal = username) | Implémenté. |
| Rôles et @PreAuthorize | Implémenté. |
| SecurityUtil (getCurrentUsername / User / UserId) | Implémenté. |
| CORS, CSRF, sessions stateless | Implémenté. |
| OTP (envoi, vérification, 2FA, reset password) | **À ajouter** : modèle/cache OTP, envoi SMS/email, endpoints et branchements dans `AuthService` / `AuthController`. |

Ce document peut servir de base pour un cahier des charges OTP (choix des cas d’usage, fournisseur SMS/email, et détails d’implémentation dans `AuthServiceImpl` et `AuthController`).
