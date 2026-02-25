package com.sneakershop.backend.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PriceBoardDTO {

    private Long variantId;
    private String productName;
    private String sku;
    private String colorway;
    private String  size;
    private BigDecimal price;
    private String symbol;
}
