package com.malitrans.transport.security;


import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.repository.UtilisateurRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    private static UtilisateurRepository utilisateurRepository;

    public SecurityUtil(UtilisateurRepository utilisateurRepository) {
        SecurityUtil.utilisateurRepository = utilisateurRepository;
    }

    /**
     * ✅ SOURCE UNIQUE DE VÉRITÉ
     * Extrait le username directement du JWT (principal = String)
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof String username) {
            return username;
        }

        throw new IllegalStateException("Unexpected authentication principal type: " + principal.getClass());
    }

    /**
     * Charge l'utilisateur depuis la DB à partir du username JWT
     */
    public static Utilisateur getCurrentUser() {
        if (utilisateurRepository == null) {
            throw new IllegalStateException("SecurityUtil not initialized");
        }

        String username = getCurrentUsername();

        return utilisateurRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated user not found in database: " + username)
                );
    }

    /**
     * Utilitaire si vraiment nécessaire (éviter autant que possible)
     */
    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
