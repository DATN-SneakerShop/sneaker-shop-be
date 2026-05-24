package com.sneakershop.backend.dto.product;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StorefrontHomeVariantResponse {
    private Long id;
    private String sku;
    private String colorway;
    private String size;
    private Integer stock;
    private Integer reservedQuantity;
    private Integer availableStock;
    private String imageUrl;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal price;
    private String status;
}