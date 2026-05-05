package com.sneakershop.backend.dto.order.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterPaymentQrResponse {
    private Long orderId;
    private String orderCode;
    private BigDecimal amount;
    private String paymentCode;
    private String bankCode;
    private String bankName;
    private String bankAccountNo;
    private String accountName;
    private String transferContent;
    private String qrImageUrl;
}
