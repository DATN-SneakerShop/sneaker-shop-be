package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Long id;
    private Long variantId;
    private String skuSnapshot;
    private String productNameSnapshot;

    private BigDecimal unitPrice;
    private Integer quantity;

    private BigDecimal lineDiscountAmount;
    private BigDecimal lineTotalAmount;

    private Integer returnedQuantity;
    private String returnNote;
}
