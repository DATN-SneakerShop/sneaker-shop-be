package com.sneakershop.backend.dto.pricing;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryDTO {
    private Long priceId;
    private BigDecimal price;
    private String symbol;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
}