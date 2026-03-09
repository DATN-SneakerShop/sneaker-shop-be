package com.sneakershop.backend.dto.pricing;

import lombok.Data;
import java.util.List;

@Data
public class ProductCampaignDTO {

    private Long id;

    private String name;

    private String brand;

    private String thumbnail;

    private List<VariantCampaignDTO> variants;

}