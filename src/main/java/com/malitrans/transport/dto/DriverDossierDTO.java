package com.malitrans.transport.dto;

import com.malitrans.transport.model.UserStatus;

import java.util.List;

public class DriverDossierDTO {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private String address;
    private String vehicleType;
    private String identityDocumentUrl;
    private String matricule;
    private UserStatus status;
    private String companyName;
    private List<GuarantorDTO> guarantors;

    public DriverDossierDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getIdentityDocumentUrl() {
        return identityDocumentUrl;
    }

    public void setIdentityDocumentUrl(String identityDocumentUrl) {
        this.identityDocumentUrl = identityDocumentUrl;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<GuarantorDTO> getGuarantors() {
        return guarantors;
    }

    public void setGuarantors(List<GuarantorDTO> guarantors) {
        this.guarantors = guarantors;
    }
}

