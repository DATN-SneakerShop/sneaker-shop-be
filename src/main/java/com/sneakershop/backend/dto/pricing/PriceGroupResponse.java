package com.sneakershop.backend.dto.pricing;

import com.sneakershop.backend.dto.promotion.PromotionDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class PriceGroupResponse {

    private Long variantId;

    private String productName;
    private String productSku;
    private String sku;

    private String colorway;

    private String size;

    private String image;

    private String brand;
    private String gender;
    private String material;
    private String model;
    private String releaseYear;
    private String description;


    private BigDecimal basePrice;

    private List<GroupPriceDTO> groupPrices;

    private List<PromotionDTO> promotions;
}