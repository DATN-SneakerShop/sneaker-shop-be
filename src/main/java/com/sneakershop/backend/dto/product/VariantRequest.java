package com.sneakershop.backend.dto.product;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantRequest {
    private Long id;

    // Có thể BE tự generate, FE không cần gửi
    private String sku;

    private String size;         // 8, 9, 10
    private String sizeType;     // US / EU / UK   ✅ BẮT BUỘC
    private String colorway;     // Black/Red

    // 🔥 FIX NÂNG CẤP: Nhận link ảnh từ Form Frontend
    private String imageUrl;

    // ✅ ĐÃ NỚI LỎNG: Frontend không gửi giá lên cũng không bị lỗi nữa
    private BigDecimal price;
    private BigDecimal salePrice;

    private int stock;
    private String status;
}