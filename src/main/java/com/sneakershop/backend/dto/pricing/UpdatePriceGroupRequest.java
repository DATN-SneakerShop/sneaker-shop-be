package com.sneakershop.backend.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePriceGroupRequest {
    private Long variantId;
    private String loaiKhach;
    private BigDecimal price;
}
