package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderSummaryDTO {
    private Long id;
    private String orderCode;
    private OrderStatus orderStatus;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    // Checklist: tính tổng doanh thu từng đơn
    private BigDecimal revenue;
    private LocalDateTime createdAt;
}
