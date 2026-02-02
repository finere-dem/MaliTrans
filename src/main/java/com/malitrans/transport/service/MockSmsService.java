package com.malitrans.transport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Mock SmsService : affiche l'OTP dans la console au lieu d'envoyer un vrai SMS.
 * Utile pour le développement et les tests.
 */
@Service
@Primary
public class MockSmsService implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(MockSmsService.class);

    @Override
    public void sendOtp(String phoneNumber, String code) {
        logger.info("[MOCK SMS] OTP for {} : {}", phoneNumber, code);
        System.out.println("=== [MOCK SMS] OTP === Phone: " + phoneNumber + " | Code: " + code + " ===");
    }
}
