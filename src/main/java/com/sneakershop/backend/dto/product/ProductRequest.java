package com.sneakershop.backend.dto.product;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    // SKU cha (BE có thể validate hoặc generate)
    @NotBlank(message = "Product SKU is required")
    private String sku;

    private String brand;
    private String model;
    private String releaseYear;

    private String gender;
    private String releaseType;
    private String status;

    private String material;
    private Boolean limited;

    private String description;
    private String thumbnail;

    @NotEmpty(message = "CategoryIds is required")
    private List<Long> categoryIds;

    @NotEmpty(message = "Variants is required")
    private List<VariantRequest> variants;
    @NotEmpty(message = "Images is required")
    private List<ProductImageRequest> images;

}
