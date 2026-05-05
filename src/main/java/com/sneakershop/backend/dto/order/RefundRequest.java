package com.sneakershop.backend.dto.order;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotNull
    private BigDecimal amount;
    private String reason;
    private String provider;
}
