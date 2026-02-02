package com.malitrans.transport.dto;

import com.malitrans.transport.model.UserStatus;
import java.time.LocalDateTime;

public class DriverSummaryDTO {
    private Long id;
    private String username;
    private String fullName;
    private String phone;
    private UserStatus status;
    private String matricule;
    private LocalDateTime createdAt;

    public DriverSummaryDTO() {
    }

    public DriverSummaryDTO(Long id, String username, String fullName, String phone, 
                          UserStatus status, String matricule, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.status = status;
        this.matricule = matricule;
        this.createdAt = createdAt;
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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getMatricule() {
        return matricule;
    }

    public void setMatricule(String matricule) {
        this.matricule = matricule;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

