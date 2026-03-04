package com.sneakershop.backend.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderItemResponse {

    private Long id;

    private Long variantId;
    private String sku;
    private String productName;
    private String size;
    private String colorway;

    // snapshot
    private String skuSnapshot;
    private String productNameSnapshot;

    private BigDecimal unitPrice;
    private Integer quantity;

    private BigDecimal lineDiscountAmount;
    private BigDecimal lineTotalAmount;

    private Integer returnedQuantity;
    private String returnNote;
    private LocalDateTime returnedAt;
}
