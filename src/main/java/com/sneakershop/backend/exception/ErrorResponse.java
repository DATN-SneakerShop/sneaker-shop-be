package com.sneakershop.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;

public class ErrorResponse {
    private String message;
    private Map<String, String> fieldErrors;

    public ErrorResponse() {}

    public ErrorResponse(String message) {
        this.message = message;
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message);
    }

    public static ErrorResponse field(String field, String message) {
        ErrorResponse response = new ErrorResponse(message);
        response.fieldErrors = new LinkedHashMap<>();
        response.fieldErrors.put(field, message);
        return response;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
}
