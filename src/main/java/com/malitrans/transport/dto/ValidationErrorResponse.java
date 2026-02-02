package com.malitrans.transport.dto;

public class ValidationErrorResponse {
    private String errorCode;
    private String message;

    public ValidationErrorResponse() {
    }

    public ValidationErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

