package com.sneakershop.backend.dto.pricing;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class GiaRequest {
    private BigDecimal gia;
    private Long tienTe; // có thể null
}
