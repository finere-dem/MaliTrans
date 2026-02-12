package com.malitrans.transport.service.impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.malitrans.transport.service.FcmService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Implémentation du service FCM.
 * Charge service-account.json au démarrage et initialise FirebaseApp de manière sécurisée.
 * Les erreurs d'envoi sont loguées sans faire crasher l'application.
 */
@Service
public class FcmServiceImpl implements FcmService {

    private static final Logger logger = LoggerFactory.getLogger(FcmServiceImpl.class);

    private static final String SERVICE_ACCOUNT_RESOURCE = "service-account.json";

    private boolean firebaseInitialized = false;

    @PostConstruct
    public void initFirebase() {
        if (FirebaseApp.getApps() != null && !FirebaseApp.getApps().isEmpty()) {
            logger.info("Firebase App already initialized, skipping.");
            firebaseInitialized = true;
            return;
        }
        try (InputStream inputStream = new ClassPathResource(SERVICE_ACCOUNT_RESOURCE).getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();
            FirebaseApp.initializeApp(options);
            firebaseInitialized = true;
            logger.info("Firebase App initialized successfully from {}", SERVICE_ACCOUNT_RESOURCE);
        } catch (IOException e) {
            logger.warn("Could not load Firebase credentials from {}. FCM will be disabled. {}", 
                    SERVICE_ACCOUNT_RESOURCE, e.getMessage());
            firebaseInitialized = false;
        }
    }

    @Override
    public boolean sendToToken(String token, String title, String body, Map<String, String> data) {
        if (token == null || token.isBlank()) {
            logger.debug("FCM send skipped: token is null or empty");
            return false;
        }
        if (!firebaseInitialized) {
            logger.debug("FCM send skipped: Firebase not initialized");
            return false;
        }
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(token.trim())
                    .setNotification(Notification.builder()
                            .setTitle(title != null ? title : "")
                            .setBody(body != null ? body : "")
                            .build());
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            String messageId = FirebaseMessaging.getInstance().send(messageBuilder.build());
            logger.debug("FCM message sent successfully, messageId={}", messageId);
            return true;
        } catch (FirebaseMessagingException e) {
            logger.error("FCM send failed for token (prefix {}...): {} - {}", 
                    token.substring(0, Math.min(20, token.length())), 
                    e.getMessagingErrorCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending FCM message", e);
            return false;
        }
    }

    /*
     * Optionnel : envoi vers un topic (à activer si besoin).
     *
     * @param topic Nom du topic FCM (ex: "drivers", "ride-123")
     * @param title Titre de la notification
     * @param body  Corps du message
     * @param data  Données additionnelles (clé/valeur string, optionnel)
     * @return true si l'envoi a réussi, false sinon
     *
    @Override
    public boolean sendToTopic(String topic, String title, String body, Map<String, String> data) {
        if (topic == null || topic.isBlank()) {
            logger.debug("FCM sendToTopic skipped: topic is null or empty");
            return false;
        }
        if (!firebaseInitialized) {
            logger.debug("FCM sendToTopic skipped: Firebase not initialized");
            return false;
        }
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setTopic(topic.trim())
                    .setNotification(Notification.builder()
                            .setTitle(title != null ? title : "")
                            .setBody(body != null ? body : "")
                            .build());
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }
            String messageId = FirebaseMessaging.getInstance().send(messageBuilder.build());
            logger.debug("FCM message sent to topic {} successfully, messageId={}", topic, messageId);
            return true;
        } catch (FirebaseMessagingException e) {
            logger.error("FCM sendToTopic failed for topic {}: {} - {}", topic, e.getErrorCode(), e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error sending FCM message to topic " + topic, e);
            return false;
        }
    }
    */
}
