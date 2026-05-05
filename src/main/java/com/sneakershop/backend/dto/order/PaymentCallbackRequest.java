package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentCallbackRequest {
    private String provider;
    private String orderCode;
    private String transactionRef;
    private String providerTransactionId;
    private String responseCode;
    private String message;
    private String signature;
    private String rawPayload;
    private BigDecimal amount;
    private Boolean success;
}
