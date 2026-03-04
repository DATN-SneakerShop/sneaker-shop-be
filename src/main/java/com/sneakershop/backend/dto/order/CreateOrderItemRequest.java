package com.sneakershop.backend.dto.order;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class CreateOrderItemRequest {
    @NotNull
    private Long variantId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
