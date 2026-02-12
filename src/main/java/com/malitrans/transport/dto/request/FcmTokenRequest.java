package com.malitrans.transport.dto.request;

/**
 * Requête pour enregistrer ou mettre à jour le token FCM de l'utilisateur connecté.
 */
public class FcmTokenRequest {

    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
