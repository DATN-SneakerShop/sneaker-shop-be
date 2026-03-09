package com.sneakershop.backend.dto.pricing;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class VariantCampaignDTO {

    private Long id;

    private String sku;

    private String colorway;

    private String size;

    private BigDecimal price;

    private Integer stock;

}