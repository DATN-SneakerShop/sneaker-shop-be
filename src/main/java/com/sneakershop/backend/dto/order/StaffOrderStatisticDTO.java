package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StaffOrderStatisticDTO {
    private Long createdById;
    private Long orderCount;
    private Long completedCount;
    private Long cancelledCount;
    private BigDecimal revenue;
}