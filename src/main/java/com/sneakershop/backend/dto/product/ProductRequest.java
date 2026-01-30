package com.sneakershop.backend.dto.product;

import lombok.Data;

@Data
public class ProductRequest {

    private String name;

    // optional – có thể bổ sung sau
    private String brand;         // Nike, Adidas
    private String gender;        // NAM / NU / UNISEX
    private String releaseType;   // RETRO / OG / LIMITED

    private String status;        // IN_STOCK / PRE_ORDER / DROP
    private String description;
    private String thumbnail;

    private Long categoryId;
}
