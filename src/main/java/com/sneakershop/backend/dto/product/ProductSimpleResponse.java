package com.sneakershop.backend.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSimpleResponse {
    private Long id;
    private String name;
    private String brand;
    private String thumbnail;
    private Long variantCount;
    private long selectedVariantCount;

    // 🔥 Tự động fix link ảnh khi gửi về Frontend
    public String getThumbnail() {
        if (thumbnail != null && !thumbnail.startsWith("http") && !thumbnail.isEmpty()) {
            return "http://localhost:8080/" + thumbnail;
        }
        return thumbnail;
    }
}