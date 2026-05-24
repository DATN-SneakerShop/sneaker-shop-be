package com.sneakershop.backend.dto.order.returning;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class CreateReturnRefundItemRequest {
    @NotNull
    private Long orderItemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String note;
}
