package com.sneakershop.backend.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class GroupPriceDTO {
    private String loaiKhach;
    private BigDecimal price;
    private BigDecimal finalPrice;
    private Integer discountPercent;

    private String promotionName;

}