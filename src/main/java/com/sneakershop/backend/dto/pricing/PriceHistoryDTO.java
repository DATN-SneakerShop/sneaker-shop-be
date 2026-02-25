package com.sneakershop.backend.dto.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceHistoryDTO(
        Long priceId,
        BigDecimal price,
        String symbol,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean active
) {}
