package com.sneakershop.backend.dto.order;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class ReturnItemRequest {
    @NotNull
    private Long orderItemId;

    @NotNull
    @Min(0)
    private Integer returnedQuantity;

    private String returnNote;
}
