package com.sneakershop.backend.dto.order.returning;

import com.sneakershop.backend.entity.order.enums.ReturnConditionStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReturnRefundInspectionResponse {
    private Long id;
    private ReturnConditionStatus conditionStatus;
    private Integer quantity;
    private Integer restockQuantity;
    private Integer refundQuantity;
    private BigDecimal refundRate;
    private BigDecimal refundAmount;
    private String responsibility;
    private String note;
}
