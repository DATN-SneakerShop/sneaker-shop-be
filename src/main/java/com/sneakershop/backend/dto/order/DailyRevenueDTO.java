package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyRevenueDTO {
    private LocalDate date;
    private Long orderCount;
    private BigDecimal revenue;
}