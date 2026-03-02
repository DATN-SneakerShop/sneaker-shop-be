package com.sneakershop.backend.dto.product;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductSearchRequest {

    /* ===== COMMON ===== */
    private String keyword;     // product.name, product.sku, variant.sku
    private List<Long> categoryIds;
    private String brand;
    private String gender;
    private String releaseType;
    private Boolean limited;

    /* ===== VARIANT ===== */
    private String size;
    private String sizeType;
    private String colorway;
    private String variantStatus;
    private String sortPrice;
    private String sort;

}
