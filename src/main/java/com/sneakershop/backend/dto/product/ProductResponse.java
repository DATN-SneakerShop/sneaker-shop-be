package com.sneakershop.backend.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private String sku;
    private String description;
    private String status;

    private Long categoryId;
    private String categoryName;

    private String thumbnail;   // URL ảnh
    private BigDecimal priceFrom;
    private Integer stock;

    private List<VariantResponse> variants;
}

