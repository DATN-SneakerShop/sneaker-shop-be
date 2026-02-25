package com.sneakershop.backend.dto.pricing;

import java.math.BigDecimal;

public class PriceResultDTO {

    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String promotionName;
    private boolean onSale;

    public PriceResultDTO(
            BigDecimal originalPrice,
            BigDecimal discountAmount,
            BigDecimal finalPrice,
            String promotionName,
            boolean onSale
    ) {
        this.originalPrice = originalPrice;
        this.discountAmount = discountAmount;
        this.finalPrice = finalPrice;
        this.promotionName = promotionName;
        this.onSale = onSale;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public boolean isOnSale() {
        return onSale;
    }
}
