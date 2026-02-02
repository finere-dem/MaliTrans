package com.malitrans.transport.service;

/**
 * Service d'envoi de SMS (OTP, notifications).
 * L'implémentation par défaut est un mock qui affiche l'OTP dans la console.
 */
public interface SmsService {

    /**
     * Envoie un code OTP au numéro donné (format international E.164, ex: +223...).
     *
     * @param phoneNumber Numéro au format international (ex: +22370123456)
     * @param code        Code OTP (ex: 6 chiffres)
     */
    void sendOtp(String phoneNumber, String code);
}
