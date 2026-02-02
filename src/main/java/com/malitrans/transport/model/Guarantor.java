package com.malitrans.transport.model;

import jakarta.persistence.*;

@Entity
public class Guarantor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // Nom du garant
    private String phone;       // Téléphone du garant
    private String address;     // Adresse du garant
    private String relation;   // Relation (e.g., "father", "friend", "brother")
    private String identityDocumentUrl; // URL/path vers le document d'identité du garant

    @ManyToOne
    private Utilisateur driver; // Le chauffeur qui a ce garant (un chauffeur doit avoir 2 garants)

    // getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public Utilisateur getDriver() {
        return driver;
    }

    public void setDriver(Utilisateur driver) {
        this.driver = driver;
    }

    public String getIdentityDocumentUrl() {
        return identityDocumentUrl;
    }

    public void setIdentityDocumentUrl(String identityDocumentUrl) {
        this.identityDocumentUrl = identityDocumentUrl;
    }
}

