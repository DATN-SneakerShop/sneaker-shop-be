package com.sneakershop.backend.dto.pricing;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CheckPriceResponse {

    private BigDecimal lowestPrice;

}