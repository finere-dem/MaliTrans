package com.malitrans.transport.dto;

public class IdentityDocumentRequest {
    private String identityDocumentUrl;

    public IdentityDocumentRequest() {
    }

    public IdentityDocumentRequest(String identityDocumentUrl) {
        this.identityDocumentUrl = identityDocumentUrl;
    }

    public String getIdentityDocumentUrl() {
        return identityDocumentUrl;
    }

    public void setIdentityDocumentUrl(String identityDocumentUrl) {
        this.identityDocumentUrl = identityDocumentUrl;
    }
}

