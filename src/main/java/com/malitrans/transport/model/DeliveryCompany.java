package com.malitrans.transport.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class DeliveryCompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    private List<Utilisateur> drivers;

    public DeliveryCompany() {
    }

    public DeliveryCompany(String name, String address) {
        this.name = name;
        this.address = address;
        this.isActive = true;
    }

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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<Utilisateur> getDrivers() {
        return drivers;
    }

    public void setDrivers(List<Utilisateur> drivers) {
        this.drivers = drivers;
    }
}

