package com.malitrans.transport.controller;

import com.malitrans.transport.dto.AuthResponse;
import com.malitrans.transport.dto.LoginRequest;
import com.malitrans.transport.dto.RefreshTokenRequest;
import com.malitrans.transport.dto.RegisterDTO;
import com.malitrans.transport.dto.VerifyRegistrationDTO;
import com.malitrans.transport.exception.TokenRefreshException;
import com.malitrans.transport.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Inscription (étape 1)", 
               description = "Crée un compte avec enabled=false et envoie un OTP par SMS. " +
                             "Le numéro est stocké au format international (ex: +223...). " +
                             "Étape 2 : appeler POST /auth/verify-registration avec le téléphone et le code reçu.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compte créé, OTP envoyé (vérifier la console en mode mock)"),
        @ApiResponse(responseCode = "400", description = "Username ou phone déjà existant, ou données invalides")
    })
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterDTO registerDTO) {
        try {
            authService.register(registerDTO);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered. Check your phone for the OTP to verify your account.");
            response.put("username", registerDTO.getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Vérification d'inscription (étape 2)", 
               description = "Valide le code OTP reçu par SMS. Si le code est valide et non expiré, " +
                             "passe l'utilisateur en enabled=true et renvoie un JWT + refresh token (connexion automatique).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Compte activé et tokens renvoyés (token, refreshToken, username, role, userId)"),
        @ApiResponse(responseCode = "400", description = "Code invalide ou expiré")
    })
    @PostMapping("/verify-registration")
    public ResponseEntity<?> verifyRegistration(@RequestBody VerifyRegistrationDTO dto) {
        try {
            AuthResponse authResponse = authService.verifyRegistration(dto.getPhone(), dto.getCode());
            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Connexion d'un utilisateur", 
               description = "Authentifie un utilisateur et retourne un access token JWT et un refresh token avec userId et role pour l'app mobile.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connexion réussie"),
        @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse authResponse = authService.login(loginRequest.getUsername(), loginRequest.getPassword());
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @Operation(summary = "Rafraîchir le token d'accès", 
               description = "Utilise un refresh token valide pour obtenir un nouveau access token. " +
                           "Le refresh token doit être valide et non expiré.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token rafraîchi avec succès"),
        @ApiResponse(responseCode = "401", description = "Refresh token invalide ou expiré")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse authResponse = authService.refreshToken(request);
            return ResponseEntity.ok(authResponse);
        } catch (TokenRefreshException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token refresh failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}

