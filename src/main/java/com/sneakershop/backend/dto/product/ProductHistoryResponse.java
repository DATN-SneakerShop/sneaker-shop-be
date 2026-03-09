package com.sneakershop.backend.dto.product;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductHistoryResponse {

    private String fieldName;

    private String oldValue;

    private String newValue;

    private LocalDateTime updatedAt;

}