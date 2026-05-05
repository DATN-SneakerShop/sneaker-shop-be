package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CartResponse {
    private Long cartId;
    private Long customerId;
    private String sessionKey;
    private List<CartItemResponse> items = new ArrayList<>();
    private Integer totalItems;
    private Integer selectedItemCount;
    private BigDecimal subtotal;
}