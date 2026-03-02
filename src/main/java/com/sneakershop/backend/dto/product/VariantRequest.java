package com.sneakershop.backend.dto.product;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VariantRequest {
    private Long id;          // 👈 thêm dòng này

    // Có thể BE tự generate, FE không cần gửi
    private String sku;

    private String size;         // 8, 9, 10
    private String sizeType;     // US / EU / UK   ✅ BẮT BUỘC

    private String colorway;     // Black/Red

    private BigDecimal price;
    private BigDecimal salePrice;

    private int stock;

    // Optional – BE đang auto theo stock
    private String status;
}
