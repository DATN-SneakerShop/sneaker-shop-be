package com.sneakershop.backend.dto.order;

import lombok.Data;

@Data
public class AddToCartRequest {
    private Long variantId;
    private Integer quantity;
}