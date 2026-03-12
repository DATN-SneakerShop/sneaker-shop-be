package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReturnedProductStatisticDTO {
    private Long variantId;
    private String skuSnapshot;
    private String productNameSnapshot;
    private Long returnedQuantity;
    private BigDecimal returnedAmount;
}