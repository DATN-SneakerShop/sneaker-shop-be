package com.sneakershop.backend.dto.product;

import lombok.Data;

@Data
public class ColorRequest {
    private String name;
    private String hexCode;
}