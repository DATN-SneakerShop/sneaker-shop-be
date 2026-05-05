package com.sneakershop.backend.dto.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantRequest {
    private Long id;
    private String sku;
    private String size;
    private String colorway;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal salePrice;
    private int stock;
    private String status;
}