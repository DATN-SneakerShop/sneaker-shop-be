package com.sneakershop.backend.dto.order;

import lombok.Data;

@Data
public class UpdateCartItemQuantityRequest {
    private Integer quantity;
}