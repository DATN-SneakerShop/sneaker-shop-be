package com.sneakershop.backend.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StorefrontHomeProductResponse {
    private Long id;
    private String productName;
    private String thumbnail;
    private String brand;
    private String gender;
    private String status;

    private List<Long> categoryIds;
    private List<String> categoryNames;

    private Long displayVariantId;
    private String displayVariantSku;
    private String displayColorway;
    private String displaySize;

    private BigDecimal originalPrice;
    private BigDecimal salePrice;

    private Boolean onSale;
    private Boolean isNew;
    private Boolean isHot;

    private String badge;
    private String detailUrl;

    private String categoryName;
    private Long defaultVariantId;
    private String createdAt;
    private List<StorefrontHomeVariantResponse> variants;
}