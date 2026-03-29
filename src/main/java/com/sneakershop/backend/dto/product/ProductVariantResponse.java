package com.sneakershop.backend.dto.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductVariantResponse {
    private Long id;
    private String sku;
    private String size;
    private String colorway;

    // 🔥 2 CỘT MỚI ĐÂY NÀY
    private String material;
    private String sole;

    private String imageUrl;
    private BigDecimal price;
    private BigDecimal salePrice;
    private int stock;
    private String status;
}