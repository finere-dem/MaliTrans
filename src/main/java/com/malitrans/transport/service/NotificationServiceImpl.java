package com.malitrans.transport.service;

import com.malitrans.transport.model.RideRequest;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.repository.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implémentation du service de notifications.
 * Envoie les notifications via FCM (Firebase Cloud Messaging) et conserve des logs pour le debug.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final UtilisateurRepository utilisateurRepository;
    private final FcmService fcmService;

    public NotificationServiceImpl(UtilisateurRepository utilisateurRepository, FcmService fcmService) {
        this.utilisateurRepository = utilisateurRepository;
        this.fcmService = fcmService;
    }

    @Override
    public void notifyDriversOfReadyRequest(RideRequest request) {
        List<Utilisateur> drivers = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CHAUFFEUR
                        && u.isEnabled()
                        && u.getStatus() != null
                        && u.getStatus().name().equals("ACTIVE"))
                .toList();

        String origin = request.getOrigin() != null ? request.getOrigin() : "?";
        String destination = request.getDestination() != null ? request.getDestination() : "?";
        String title = "Nouvelle Course Disponible !";
        String body = "De " + origin + " vers " + destination;

        Map<String, String> data = new HashMap<>();
        data.put("rideId", request.getId().toString());
        data.put("type", "NEW_RIDE");

        for (Utilisateur driver : drivers) {
            String fcmToken = driver.getFcmToken();
            if (fcmToken == null || fcmToken.isBlank()) {
                logger.debug("Driver {} has no FCM token, skipping push", driver.getId());
                continue;
            }
            boolean sent = fcmService.sendToToken(fcmToken, title, body, data);
            if (!sent) {
                logger.warn("FCM send failed for driver {} (rideId={}), continuing with other drivers", driver.getId(), request.getId());
            }
        }

        logger.info("Notified {} driver(s) of ready request id={} ({} → {})", drivers.size(), request.getId(), origin, destination);
    }

    @Override
    public void notifySupplierForValidation(RideRequest request) {
        Utilisateur supplier = request.getSupplier();
        if (supplier == null) {
            logger.debug("No supplier on request id={}, skipping FCM", request.getId());
            return;
        }
        String fcmToken = supplier.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            logger.debug("Supplier {} has no FCM token, skipping push", supplier.getId());
            return;
        }

        String title = "Validation requise";
        String body = "Une demande de course vous attend. Validez-la pour permettre la collecte. #" + request.getId();

        Map<String, String> data = new HashMap<>();
        data.put("rideId", request.getId().toString());
        data.put("type", "VALIDATION_REQUIRED");

        boolean sent = fcmService.sendToToken(fcmToken, title, body, data);
        if (!sent) {
            logger.warn("FCM send failed for supplier {} (rideId={})", supplier.getId(), request.getId());
        } else {
            logger.info("Notified supplier {} for validation of request id={}", supplier.getId(), request.getId());
        }
    }

    @Override
    public void notifyClientForValidation(RideRequest request) {
        Utilisateur client = request.getClient();
        if (client == null) {
            logger.debug("No client on request id={}, skipping FCM", request.getId());
            return;
        }
        String fcmToken = client.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            logger.debug("Client {} has no FCM token, skipping push", client.getId());
            return;
        }

        String title = "Validation requise";
        String body = "Une demande de livraison vous attend. Validez-la pour lancer la course. #" + request.getId();

        Map<String, String> data = new HashMap<>();
        data.put("rideId", request.getId().toString());
        data.put("type", "VALIDATION_REQUIRED");

        boolean sent = fcmService.sendToToken(fcmToken, title, body, data);
        if (!sent) {
            logger.warn("FCM send failed for client {} (rideId={})", client.getId(), request.getId());
        } else {
            logger.info("Notified client {} for validation of request id={}", client.getId(), request.getId());
        }
    }

    @Override
    public void notifyDriverOfAssignment(RideRequest request) {
        Utilisateur driver = request.getChauffeur();
        if (driver == null) {
            logger.debug("No driver assigned on request id={}, skipping FCM", request.getId());
            return;
        }
        String fcmToken = driver.getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            logger.debug("Driver {} has no FCM token, skipping push", driver.getId());
            return;
        }

        String title = "Course confirmée";
        String body = "Vous avez été assigné à la course #" + request.getId();

        Map<String, String> data = new HashMap<>();
        data.put("rideId", request.getId().toString());
        data.put("type", "ASSIGNED");

        boolean sent = fcmService.sendToToken(fcmToken, title, body, data);
        if (!sent) {
            logger.warn("FCM send failed for driver {} (rideId={})", driver.getId(), request.getId());
        } else {
            logger.info("Notified driver {} of assignment to request id={}", driver.getId(), request.getId());
        }
    }
}
