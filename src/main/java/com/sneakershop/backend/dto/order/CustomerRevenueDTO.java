package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerRevenueDTO {
    private Long customerId;
    private Long orderCount;
    private BigDecimal revenue;
}