package com.sneakershop.backend.dto.product;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantResponse {

    private Long id;
    private String sku;
    private String size;
    private String colorway;

    // 🔥 CỔNG CHÍNH: Nhả link ảnh riêng của biến thể về cho Frontend
    private String imageUrl;

    private BigDecimal price;
    private BigDecimal salePrice;

    private int stock;
    private String status;
}