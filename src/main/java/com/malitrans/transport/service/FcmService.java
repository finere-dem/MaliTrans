package com.malitrans.transport.service;

import java.util.Map;

/**
 * Service technique pour l'envoi de notifications push via Firebase Cloud Messaging (FCM).
 */
public interface FcmService {

    /**
     * Envoie une notification à un utilisateur via son token FCM.
     *
     * @param token Token FCM du dispositif (non null, non vide)
     * @param title Titre de la notification
     * @param body  Corps du message
     * @param data  Données additionnelles (clé/valeur string, optionnel, peut être null)
     * @return true si l'envoi a réussi, false sinon (erreur loguée, l'appli ne crash pas)
     */
    boolean sendToToken(String token, String title, String body, Map<String, String> data);

    // --- Optionnel (à activer plus tard) ---
    // boolean sendToTopic(String topic, String title, String body, Map<String, String> data);
}
