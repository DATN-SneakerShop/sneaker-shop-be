package com.sneakershop.backend.dto.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantResponse {
    private Long variantId;
    private String sku;
    private String colorway;
    private String size;

    private BigDecimal salePrice;

    private BigDecimal price;
    private Integer stock;
    private Integer reservedQuantity;
    private Integer availableStock;
    private String imageUrl;
}