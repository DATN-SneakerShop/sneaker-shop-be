package com.sneakershop.backend.dto.order;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CancelOrderRequest {
    @NotBlank
    private String reason;
    private Long cancelledById; // optional
}
