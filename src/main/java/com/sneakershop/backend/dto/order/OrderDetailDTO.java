package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailDTO {
    private Long id;
    private String orderCode;

    private Long customerId;
    private Long createdById;

    private SalesChannel channel;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private ReturnStatus returnStatus;

    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private BigDecimal returnedAmount;
    private BigDecimal finalAmount;

    // Checklist: tính tổng doanh thu từng đơn
    private BigDecimal revenue;

    private String note;
    private String returnNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<OrderItemDTO> items;
}
