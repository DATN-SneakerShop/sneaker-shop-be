package com.sneakershop.backend.dto.product;

import lombok.Data;

@Data
public class ProductImageResponse {

    private Long id;
    private String url;
    private boolean isThumbnail;
}
