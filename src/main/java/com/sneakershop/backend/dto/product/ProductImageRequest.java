package com.sneakershop.backend.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProductImageRequest {
    @JsonProperty("url")
    private String imageUrl;

    private boolean isThumbnail;
}
