package com.sneakershop.backend.dto.promotion;

import com.sneakershop.backend.entity.promotion.DiscountType;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PromotionDetailRequest {
    private Long variantId;
    private DiscountType discountType;
    private BigDecimal discountValue;
}