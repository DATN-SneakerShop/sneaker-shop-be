package com.sneakershop.backend.dto.product;

import lombok.Data;

@Data
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String thumbnail;   //
}
