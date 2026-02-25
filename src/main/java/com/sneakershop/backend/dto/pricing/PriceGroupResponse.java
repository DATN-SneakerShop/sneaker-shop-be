package com.sneakershop.backend.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class PriceGroupResponse {
    private Long variantId;
    private String productName;
    private String sku;
    private String colorway;
    private String  size;
    private BigDecimal basePrice;
    private List<GroupPriceDTO> groupPrices;
}