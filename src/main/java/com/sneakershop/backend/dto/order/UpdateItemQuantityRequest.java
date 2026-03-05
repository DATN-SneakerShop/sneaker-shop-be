package com.sneakershop.backend.dto.order;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class UpdateItemQuantityRequest {
    @NotNull
    @Min(1)
    private Integer quantity;
}
