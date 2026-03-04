package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(toBuilder = true)
public class OrderResponse {

    private Long id;
    private String orderCode;

    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerType;

    private Long createdById;
    private String createdByUsername;
    private String createdByFullName;

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

    private String currencyCode;

    private String promotionCode;
    private BigDecimal promotionDiscountAmount;

    private String shippingCarrier;
    private String trackingCode;

    private String note;
    private String returnNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime returnedAt;

    private String cancelReason;
    private Long cancelledById;

    private Boolean emailSent;
    private LocalDateTime emailSentAt;

    private List<OrderItemResponse> items;
}
