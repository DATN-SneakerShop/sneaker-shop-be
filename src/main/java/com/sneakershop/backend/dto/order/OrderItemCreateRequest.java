package com.sneakershop.backend.dto.order;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class OrderItemCreateRequest {
    @NotNull
    private Long variantId;

    @NotNull
    @Min(1)
    private Integer quantity;

    // Để không đụng module product: FE truyền unitPrice + snapshot
    private BigDecimal unitPrice;

    private String skuSnapshot;
    private String productNameSnapshot;

    private BigDecimal lineDiscountAmount;
}
