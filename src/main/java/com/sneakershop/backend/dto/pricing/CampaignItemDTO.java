package com.sneakershop.backend.dto.pricing;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CampaignItemDTO {

    private Long variantId;

    private String productName;

    private String sku;

    private String colorway;

    private String size;

    private String image;

    private BigDecimal originalPrice;

    private BigDecimal price;

}