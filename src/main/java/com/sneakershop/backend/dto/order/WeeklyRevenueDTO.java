package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WeeklyRevenueDTO {
    private String weekLabel;
    private Long orderCount;
    private BigDecimal revenue;
}