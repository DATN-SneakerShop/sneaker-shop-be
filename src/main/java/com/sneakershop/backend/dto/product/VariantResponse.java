package com.sneakershop.backend.dto.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantResponse {
    private Long variantId;
    private String sku;
    private String colorway;
    private String size;

    // 👇 Thêm 3 trường này để hết báo lỗi đỏ ở ProductService
    private String material;
    private String sole;
    private BigDecimal salePrice;

    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
}