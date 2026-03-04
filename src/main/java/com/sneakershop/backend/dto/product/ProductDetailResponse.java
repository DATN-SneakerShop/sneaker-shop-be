package com.sneakershop.backend.dto.product;

import lombok.Data;
import java.util.List;

@Data
public class ProductDetailResponse {

    private Long id;
    private String name;
    private String sku;

    private String brand;
    private String model;
    private String releaseYear;

    private String gender;
    private String releaseType;
    private String status;

    private String material;
    private Boolean limited;

    private String description;
    private String thumbnail; // ✅ Đã thêm để fix lỗi setThumbnail

    /* ================== CATEGORY ================== */
    private List<Long> categoryIds;
    private List<String> categoryNames;

    /* ================== IMAGES ================== */
    private List<ProductImageResponse> images;

    /* ================== VARIANTS ================== */
    private List<ProductVariantResponse> variants;
}