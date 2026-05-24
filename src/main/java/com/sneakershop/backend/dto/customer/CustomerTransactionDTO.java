package com.sneakershop.backend.dto.customer;

import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerTransactionDTO {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long orderId;
    private String orderCode;
    private LocalDateTime createdAt;
    private BigDecimal orderAmount;
    private BigDecimal returnedAmount;
    private BigDecimal netAmount;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private SalesChannel channel;
    private String type;
}
