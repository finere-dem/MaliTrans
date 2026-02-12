package com.malitrans.transport.service;

import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    public UtilisateurService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public Optional<Utilisateur> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return utilisateurRepository.findById(id);
    }

    public Optional<Utilisateur> findByUsername(String username) {
        return utilisateurRepository.findByUsername(username);
    }

    public List<Utilisateur> findByRole(Role role) {
        return utilisateurRepository.findByRole(role);
    }

    /**
     * Met à jour le token FCM de l'utilisateur identifié par son username.
     *
     * @param username Username de l'utilisateur (ex: issu du JWT)
     * @param token    Nouveau token FCM (peut être null pour révoquer)
     */
    public void updateFcmToken(String username, String token) {
        Utilisateur user = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        user.setFcmToken(token);
        utilisateurRepository.save(user);
    }
}
