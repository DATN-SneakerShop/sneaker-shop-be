package com.sneakershop.backend.dto.order;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CancelOrderRequest {
    @NotBlank(message = "Reason must not be blank")
    private String reason;

    private Long cancelledById;
}