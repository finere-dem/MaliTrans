package com.malitrans.transport.dto;

import java.time.LocalDateTime;

public class RideRequestDTO {
    private Long id;
    private String origin;
    private String destination;
    private Long clientId;
    private Long supplierId;
    private Long chauffeurId;
    private String flowType; // "CLIENT_INITIATED" or "SUPPLIER_INITIATED"
    private String validationStatus; // ValidationStatus enum as String
    private String qrCodePickup;
    private String qrCodeDelivery;
    private Double price;
    private String otherPartyName;  // Nom de l'autre partie (Recipient si isSenderClient=true, Sender/Supplier si false)
    private String otherPartyPhone;  // Téléphone de l'autre partie
    private Boolean isSenderClient;  // true si le Client envoie, false si le Client reçoit
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Long getChauffeurId() { return chauffeurId; }
    public void setChauffeurId(Long chauffeurId) { this.chauffeurId = chauffeurId; }

    public String getFlowType() { return flowType; }
    public void setFlowType(String flowType) { this.flowType = flowType; }

    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }

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
