package com.sneakershop.backend.dto.customer;

import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class CustomerHistoryDTO {

    private Long customerId;
    private String customerName;
    private String customerEmail;

    private Long orderId;
    private String orderCode;
    private LocalDateTime createdAt;
    private BigDecimal finalAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;

    public CustomerHistoryDTO(
            Long customerId,
            String customerName,
            String customerEmail,
            Long orderId,
            String orderCode,
            LocalDateTime createdAt,
            BigDecimal finalAmount,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus
    ) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.createdAt = createdAt;
        this.finalAmount = finalAmount;
        this.orderStatus = orderStatus;
        this.paymentStatus = paymentStatus;
    }
}