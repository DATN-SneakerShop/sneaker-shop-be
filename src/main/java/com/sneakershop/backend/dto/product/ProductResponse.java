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
    private String brand;
    private String gender;
    private String releaseType;

    private String thumbnail;        // URL ảnh
    private Boolean isNew;
    private Boolean isHot;
    private BigDecimal discountedPrice;
    private BigDecimal priceFrom;    // giá thấp nhất (optional, FE dùng list)

    // ✅ NHIỀU CATEGORY
    private List<Long> categoryIds;
    private List<String> categoryNames;
    private List<ProductImageResponse> images;
    private List<ProductVariantResponse> variants;
    private List<String> tags;
}
