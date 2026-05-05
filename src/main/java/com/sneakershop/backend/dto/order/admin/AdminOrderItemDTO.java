package com.sneakershop.backend.dto.order.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminOrderItemDTO {
    private Long id;
    private Long variantId;
    private Long productIdSnapshot;
    private Long variantIdSnapshot;

    private String imageUrlSnapshot;
    private String productNameSnapshot;
    private String skuSnapshot;
    private String colorSnapshot;
    private String sizeSnapshot;
    private String materialSnapshot;
    private String soleSnapshot;

    private Integer quantity;
    private Integer returnedQuantity;

    private BigDecimal baseUnitPrice;
    private BigDecimal unitPrice;
    private BigDecimal promotionDiscountAmount;
    private BigDecimal lineDiscountAmount;
    private BigDecimal lineTotalAmount;

    private String returnNote;
}
