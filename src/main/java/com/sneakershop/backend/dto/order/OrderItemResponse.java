package com.sneakershop.backend.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private Long variantId;
    private String skuSnapshot;
    private String productNameSnapshot;
    private String colorSnapshot;
    private String sizeSnapshot;
    private String materialSnapshot;
    private String soleSnapshot;
    private String imageUrlSnapshot;
    private BigDecimal baseUnitPrice;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal promotionDiscountAmount;
    private BigDecimal lineDiscountAmount;
    private BigDecimal lineTotalAmount;
    private Integer returnedQuantity;
}
