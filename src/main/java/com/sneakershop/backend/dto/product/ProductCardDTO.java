package com.sneakershop.backend.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ProductCardDTO {

    private Long variantId;
    private String productName;
    private String sku;
    private String colorway;
    private String size;

    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private BigDecimal discountValue;

    private String promotionName;
    private Integer discountPercent;
    private boolean onSale;
}
