package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReturnReportDTO {
    private Long orderId;
    private String orderCode;
    private ReturnStatus returnStatus;
    private BigDecimal totalAmount;
    private BigDecimal returnedAmount;
    private BigDecimal finalAmount;
    private LocalDateTime returnedAt;
}
