package com.malitrans.transport.model;

/**
 * Statut du compte utilisateur (ex: chauffeur).
 * Pour les chauffeurs : seuls les comptes ACTIVE peuvent voir et accepter les courses.
 * PENDING_COMPANY_VERIFICATION → PENDING_ADMIN_APPROVAL → ACTIVE (= validation complète).
 */
public enum UserStatus {
    PENDING_COMPANY_VERIFICATION,  // Enregistré, en attente de vérification par l'entreprise
    PENDING_ADMIN_APPROVAL,         // Vérifié par l'entreprise, en attente d'approbation Admin
    ACTIVE,                          // Validation complète : peut voir et accepter les courses
    SUSPENDED,                       // Suspendu
    REJECTED,                        // Rejeté
    PENDING_VALIDATION               // Legacy - pour compatibilité avec l'ancien système
}

