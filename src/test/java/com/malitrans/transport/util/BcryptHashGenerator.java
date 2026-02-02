package com.malitrans.transport.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Génère un hash BCrypt pour le mot de passe des managers (script SQL).
 * Exécuter ce test pour afficher le hash à mettre dans data-mali-companies.sql
 */
class BcryptHashGenerator {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Test
    void printBcryptHashForPassword123() {
        String rawPassword = "password123";
        String hash = ENCODER.encode(rawPassword);
        System.out.println("Hash BCrypt pour '" + rawPassword + "' :");
        System.out.println(hash);
        // Vérification
        boolean matches = ENCODER.matches(rawPassword, hash);
        System.out.println("Vérification matches: " + matches);
    }
}
