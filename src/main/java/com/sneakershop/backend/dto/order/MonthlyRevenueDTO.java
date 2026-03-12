package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyRevenueDTO {
    private String month;
    private Long orderCount;
    private BigDecimal revenue;
}