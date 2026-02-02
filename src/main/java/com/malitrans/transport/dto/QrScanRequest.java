package com.malitrans.transport.dto;

public class QrScanRequest {
    // driverId removed - extracted from JWT token for security
    private String qrCode;
    private String type; // "PICKUP" or "DELIVERY"

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

