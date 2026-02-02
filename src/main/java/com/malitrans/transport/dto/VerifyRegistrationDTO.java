package com.malitrans.transport.dto;

/**
 * Corps de la requête POST /auth/verify-registration.
 */
public class VerifyRegistrationDTO {

    private String phone;
    private String code;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
