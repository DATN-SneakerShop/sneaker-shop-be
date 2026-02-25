package com.sneakershop.backend.dto.pricing;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Setter
public class PricingCalculateRequest {

    @NotNull
    private Long variantId;

    @Positive
    private int quantity;
}
