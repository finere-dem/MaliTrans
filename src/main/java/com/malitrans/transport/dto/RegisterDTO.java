package com.malitrans.transport.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public class RegisterDTO {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    /** Téléphone : accepte "phone" ou "phoneNumber" (compatibilité app Flutter). */
    @JsonAlias("phoneNumber")
    private String phone;
    private String email; // Optional email field
    private String role; // "CLIENT" or "DRIVER" (will be converted to CHAUFFEUR)
    private String vehicleType; // Optional, for drivers
    private Long companyId; // Required for CHAUFFEUR role

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}
