package com.malitrans.transport.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class RideRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String origin;
    private String destination;

    @ManyToOne
    private Utilisateur client;

    @ManyToOne
    private Utilisateur supplier;  // Le fournisseur (shop/vendor) - nullable in P2P model

    @ManyToOne
    private Utilisateur chauffeur; // Le chauffeur assigné

    @Enumerated(EnumType.STRING)
    private FlowType flowType;  // CLIENT_INITIATED ou SUPPLIER_INITIATED

    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus;  // Statut de validation

    private String qrCodePickup;    // Token pour validation par le fournisseur
    private String qrCodeDelivery;  // Token pour validation par le client

    private Double price;  // Prix fixe (plus de négociation)

    private String otherPartyName;  // Nom de l'autre partie (Recipient si isSenderClient=true, Sender/Supplier si false)
    private String otherPartyPhone; // Téléphone de l'autre partie
    private Boolean isSenderClient; // true si le Client envoie, false si le Client reçoit

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Utilisateur getClient() { return client; }
    public void setClient(Utilisateur client) { this.client = client; }

    public Utilisateur getSupplier() { return supplier; }
    public void setSupplier(Utilisateur supplier) { this.supplier = supplier; }

    public Utilisateur getChauffeur() { return chauffeur; }
    public void setChauffeur(Utilisateur chauffeur) { this.chauffeur = chauffeur; }

    public FlowType getFlowType() { return flowType; }
    public void setFlowType(FlowType flowType) { this.flowType = flowType; }

    public ValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(ValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public String getQrCodePickup() { return qrCodePickup; }
    public void setQrCodePickup(String qrCodePickup) { this.qrCodePickup = qrCodePickup; }

    public String getQrCodeDelivery() { return qrCodeDelivery; }
    public void setQrCodeDelivery(String qrCodeDelivery) { this.qrCodeDelivery = qrCodeDelivery; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getOtherPartyName() { return otherPartyName; }
    public void setOtherPartyName(String otherPartyName) { this.otherPartyName = otherPartyName; }

    public String getOtherPartyPhone() { return otherPartyPhone; }
    public void setOtherPartyPhone(String otherPartyPhone) { this.otherPartyPhone = otherPartyPhone; }

    public Boolean getIsSenderClient() { return isSenderClient; }
    public void setIsSenderClient(Boolean isSenderClient) { this.isSenderClient = isSenderClient; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
