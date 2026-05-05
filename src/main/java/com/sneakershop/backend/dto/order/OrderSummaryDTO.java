package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderSummaryDTO {
    private Long id;
    private String orderCode;
    private Long customerId;
    private Long createdById;
    private SalesChannel channel;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private BigDecimal revenue;
    private LocalDateTime createdAt;
    
}