package com.sneakershop.backend.dto.promotion;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PromotionVariantDTO {

    private Long variantId;
    private String productName;
    private String color;
    private Integer size;

    /** Giá gốc */
    private BigDecimal price;

    /** Giá sau giảm */
    private BigDecimal discountedPrice;

    /** ✅ Tồn kho */
    private Integer stock;

    /** 🔥 ĐÃ ĐỔI TÊN THÀNH thumbnail ĐỂ FRONTEND NHẬN ĐƯỢC ẢNH */
    private String thumbnail;
}