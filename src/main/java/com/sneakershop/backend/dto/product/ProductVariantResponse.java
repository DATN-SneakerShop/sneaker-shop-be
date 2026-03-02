package com.sneakershop.backend.dto.product;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantResponse {

    private Long id;
    private String sku;
    private String size;
    private String sizeType;
    private String colorway;

    private BigDecimal price;
    private BigDecimal salePrice;

    private int stock;
    private String status;
}
