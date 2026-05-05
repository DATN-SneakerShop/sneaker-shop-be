package com.sneakershop.backend.dto.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInitResponse {
    private String provider;
    private String paymentUrl;
    private String transactionRef;
}
