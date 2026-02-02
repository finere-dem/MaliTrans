package com.malitrans.transport.service;

import com.malitrans.transport.model.RideRequest;
import com.malitrans.transport.model.Role;
import com.malitrans.transport.model.Utilisateur;
import com.malitrans.transport.model.ValidationStatus;
import com.malitrans.transport.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mock implementation of NotificationService.
 * Uses System.out.println for logging notifications.
 * 
 * TODO: Replace with Firebase Cloud Messaging (FCM) integration
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private final UtilisateurRepository utilisateurRepository;

    public NotificationServiceImpl(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    public void notifyDriversOfReadyRequest(RideRequest request) {
        // Get all active drivers (chauffeurs)
        List<Utilisateur> drivers = utilisateurRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.CHAUFFEUR 
                        && u.isEnabled() 
                        && u.getStatus() != null 
                        && u.getStatus().name().equals("ACTIVE"))
                .toList();

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("MOCK NOTIFICATION: Ride Request Ready for Pickup");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Request ID: " + request.getId());
        System.out.println("Route: " + request.getOrigin() + " → " + request.getDestination());
        System.out.println("Client: " + (request.getClient() != null ? request.getClient().getFullName() : "N/A"));
        System.out.println("Supplier: " + (request.getSupplier() != null ? request.getSupplier().getFullName() : "N/A"));
        System.out.println("Price: " + request.getPrice() + " FCFA");
        System.out.println("Status: " + request.getValidationStatus());
        System.out.println("Notifying " + drivers.size() + " active driver(s)...");
        
        for (Utilisateur driver : drivers) {
            String fcmToken = driver.getFcmToken();
            System.out.println("  → Sending to Driver: " + driver.getFullName() + 
                             " (ID: " + driver.getId() + 
                             (fcmToken != null ? ", FCM Token: " + fcmToken.substring(0, Math.min(20, fcmToken.length())) + "..." : ", No FCM Token") + ")");
        }
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    @Override
    public void notifySupplierForValidation(RideRequest request) {
        Utilisateur supplier = request.getSupplier();

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("MOCK NOTIFICATION: Validation Required (Supplier)");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Request ID: " + request.getId());
        System.out.println("Flow Type: CLIENT_INITIATED");
        System.out.println("Route: " + request.getOrigin() + " → " + request.getDestination());
        System.out.println("Client: " + (request.getClient() != null ? request.getClient().getFullName() : "N/A"));
        System.out.println("Price: " + request.getPrice() + " FCFA");
        System.out.println("QR Code Pickup: " + request.getQrCodePickup());
        
        if (supplier != null) {
            String fcmToken = supplier.getFcmToken();
            System.out.println("Notifying Supplier: " + supplier.getFullName() + 
                             " (ID: " + supplier.getId() + 
                             (fcmToken != null ? ", FCM Token: " + fcmToken.substring(0, Math.min(20, fcmToken.length())) + "..." : ", No FCM Token") + ")");
            System.out.println("  → Please validate this request to proceed.");
        }
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    @Override
    public void notifyClientForValidation(RideRequest request) {
        Utilisateur client = request.getClient();

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("MOCK NOTIFICATION: Validation Required (Client)");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Request ID: " + request.getId());
        System.out.println("Flow Type: SUPPLIER_INITIATED");
        System.out.println("Route: " + request.getOrigin() + " → " + request.getDestination());
        System.out.println("Supplier: " + (request.getSupplier() != null ? request.getSupplier().getFullName() : "N/A"));
        System.out.println("Price: " + request.getPrice() + " FCFA");
        System.out.println("QR Code Delivery: " + request.getQrCodeDelivery());
        
        if (client != null) {
            String fcmToken = client.getFcmToken();
            System.out.println("Notifying Client: " + client.getFullName() + 
                             " (ID: " + client.getId() + 
                             (fcmToken != null ? ", FCM Token: " + fcmToken.substring(0, Math.min(20, fcmToken.length())) + "..." : ", No FCM Token") + ")");
            System.out.println("  → Please validate this request to proceed.");
        }
        System.out.println("═══════════════════════════════════════════════════════════");
    }

    @Override
    public void notifyDriverOfAssignment(RideRequest request) {
        Utilisateur driver = request.getChauffeur();

        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("MOCK NOTIFICATION: Delivery Assigned!");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Request ID: " + request.getId());
        System.out.println("Route: " + request.getOrigin() + " → " + request.getDestination());
        System.out.println("Client: " + (request.getClient() != null ? request.getClient().getFullName() : "N/A"));
        System.out.println("Supplier: " + (request.getSupplier() != null ? request.getSupplier().getFullName() : "N/A"));
        System.out.println("Price: " + request.getPrice() + " FCFA");
        System.out.println("QR Code Pickup: " + request.getQrCodePickup());
        System.out.println("QR Code Delivery: " + request.getQrCodeDelivery());
        
        if (driver != null) {
            String fcmToken = driver.getFcmToken();
            System.out.println("Notifying Driver: " + driver.getFullName() + 
                             " (ID: " + driver.getId() + 
                             (fcmToken != null ? ", FCM Token: " + fcmToken.substring(0, Math.min(20, fcmToken.length())) + "..." : ", No FCM Token") + ")");
            System.out.println("  → You have been assigned to this delivery. Please proceed to pickup.");
        }
        System.out.println("═══════════════════════════════════════════════════════════");
    }
}

