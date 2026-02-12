package com.malitrans.transport.model;

import jakarta.persistence.*;
import java.util.Set;

@Entity
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String phone; // Numéro de téléphone
    private String vehicleType; // e.g., "Moto", "Voiture", "Camion"
    private Double rating; // Average rating from notes (0.0 to 5.0)
    @Column(length = 512)
    private String fcmToken; // Firebase Cloud Messaging token for push notifications
    private String companyName; // Nom de l'entreprise (pour les Suppliers)
    private String address; // Adresse
    private String identityDocumentUrl; // URL/path vers le document d'identité (pour les Drivers)
    private String matricule; // Code interne unique généré lors de la validation par la compagnie

    @Enumerated(EnumType.STRING)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private DeliveryCompany company;  // Entreprise de livraison (pour les chauffeurs)

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.PENDING_VALIDATION; // Status de l'utilisateur

    private boolean enabled = true;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return username;
        }
        if (firstName == null) return lastName != null ? lastName : username;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getIdentityDocumentUrl() {
        return identityDocumentUrl;
    }

    public void setIdentityDocumentUrl(String identityDocumentUrl) {
        this.identityDocumentUrl = identityDocumentUrl;
    }

    public DeliveryCompany getCompany() {
        return company;
    }

    public void setCompany(DeliveryCompany company) {
        this.company = company;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }
}
