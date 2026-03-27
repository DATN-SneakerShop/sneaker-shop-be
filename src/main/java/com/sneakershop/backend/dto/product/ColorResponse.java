package com.sneakershop.backend.dto.product;

import lombok.Data;

@Data
public class ColorResponse {
    private Long id;
    private String name;
    private String hexCode;
}