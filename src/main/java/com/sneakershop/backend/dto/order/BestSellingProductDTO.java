package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BestSellingProductDTO {
    private Long variantId;
    private String skuSnapshot;
    private String productNameSnapshot;
    private Long totalQuantity;
    private Long returnedQuantity;
    private Long netQuantity;
    private BigDecimal revenue;
}