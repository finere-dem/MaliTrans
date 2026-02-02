package com.malitrans.transport.dto;

import java.util.List;

public class DriverValidationDTO {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private String vehicleType;
    private String identityDocumentUrl;
    private String status;
    private List<GuarantorDTO> guarantors;

    public DriverValidationDTO() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<GuarantorDTO> getGuarantors() {
        return guarantors;
    }

    public void setGuarantors(List<GuarantorDTO> guarantors) {
        this.guarantors = guarantors;
    }
}

