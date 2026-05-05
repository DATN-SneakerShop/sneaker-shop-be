package com.sneakershop.backend.dto.order.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminOrderPaymentHistoryDTO {
    private Long transactionId;
    private String provider;
    private String providerTransactionId;
    private String referenceCode;
    private String message;
    private BigDecimal amount;
    private BigDecimal actualAmount;
    private BigDecimal transferAmount;
    private BigDecimal runningAmount;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime receivedAt;
}
