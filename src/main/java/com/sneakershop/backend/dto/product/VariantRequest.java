package com.sneakershop.backend.dto.product;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantRequest {

    private String sku;
    private String size;       // US 8, 9, 10
    private String colorway;   // Black/Red

    private BigDecimal price;
    private BigDecimal salePrice;

    private int stock;
    private String status;     // IN_STOCK / OUT_OF_STOCK
}
