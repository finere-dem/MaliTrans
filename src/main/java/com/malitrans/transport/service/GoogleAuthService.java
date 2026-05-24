package com.malitrans.transport.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.malitrans.transport.model.RefreshToken;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.UserStatus;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.repository.UtilisateurRepository;
import com.malitrans.transport.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleAuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final RefreshTokenService refreshTokenService;
    private final String googleClientId;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public GoogleAuthService(UtilisateurRepository utilisateurRepository,
                             PasswordEncoder passwordEncoder,
                             JwtTokenUtil jwtTokenUtil,
                             RefreshTokenService refreshTokenService,
                             @Value("${google.client-id:}") String googleClientId) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.refreshTokenService = refreshTokenService;
        this.googleClientId = googleClientId;
        this.googleIdTokenVerifier = buildVerifier(googleClientId);
    }

    @Transactional
    public Map<String, Object> authenticate(String idTokenString) {
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new BadCredentialsException("Token Google manquant.");
        }
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("Google client ID non configure.");
        }

        GoogleIdToken.Payload payload = verify(idTokenString);
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = readPayloadString(payload, "name");
        String firstName = readPayloadString(payload, "given_name");
        String lastName = readPayloadString(payload, "family_name");

        if (email == null || email.isBlank()) {
            throw new BadCredentialsException("Aucun email Google n'a ete fourni.");
        }
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BadCredentialsException("L'email Google n'est pas verifie.");
        }

        Utilisateur utilisateur = findOrCreateGoogleUser(googleId, email, name, firstName, lastName);
        String roleString = utilisateur.getRole() != null ? utilisateur.getRole().name() : Role.CLIENT.name();
        String accessToken = jwtTokenUtil.generateToken(utilisateur.getUsername(), List.of(roleString));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(utilisateur.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", accessToken);
        response.put("refreshToken", refreshToken.getToken());
        response.put("username", utilisateur.getUsername());
        response.put("role", roleString);
        response.put("userId", utilisateur.getId());
        response.put("id", utilisateur.getId());
        response.put("nom", utilisateur.getFullName());
        response.put("email", utilisateur.getEmail());
        response.put("firstName", utilisateur.getFirstName());
        response.put("lastName", utilisateur.getLastName());
        return response;
    }

    private GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);
            if (idToken == null) {
                throw new BadCredentialsException("Token Google invalide.");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new BadCredentialsException("Impossible de verifier le token Google.", e);
        }
    }

    private GoogleIdTokenVerifier buildVerifier(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return null;
        }

        try {
            return new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(List.of(clientId))
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Impossible d'initialiser Google Sign-In.", e);
        }
    }

    private Utilisateur findOrCreateGoogleUser(String googleId, String email, String name, String firstName,
                                               String lastName) {
        Optional<Utilisateur> byGoogleId = utilisateurRepository.findByGoogleId(googleId);
        if (byGoogleId.isPresent()) {
            return byGoogleId.get();
        }

        Optional<Utilisateur> byEmail = utilisateurRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            Utilisateur existing = byEmail.get();
            if (existing.getGoogleId() == null || existing.getGoogleId().isBlank()) {
                existing.setGoogleId(googleId);
            }
            existing.setEnabled(true);
            return utilisateurRepository.save(existing);
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername(email);
        utilisateur.setEmail(email);
        utilisateur.setGoogleId(googleId);
        utilisateur.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        utilisateur.setRole(Role.CLIENT);
        utilisateur.setEnabled(true);
        utilisateur.setStatus(UserStatus.ACTIVE);

        if (firstName != null && !firstName.isBlank()) {
            utilisateur.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            utilisateur.setLastName(lastName);
        }
        if ((utilisateur.getFirstName() == null || utilisateur.getFirstName().isBlank())
                && name != null && !name.isBlank()) {
            utilisateur.setFirstName(name);
        }

        return utilisateurRepository.save(utilisateur);
    }

    private String readPayloadString(GoogleIdToken.Payload payload, String key) {
        Object value = payload.get(key);
        return value != null ? value.toString() : null;
    }
}
