package com.sneakershop.backend.dto.order.storefront;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StorefrontOrderSummaryResponse {
    private Long id;
    private String orderCode;
    private String lookupCode;
    private String orderStatus;
    private String paymentStatus;
    private String shippingStatus;
    private String paymentMethod;
    private BigDecimal finalAmount;
    private Integer totalItems;
    private LocalDateTime createdAt;
}