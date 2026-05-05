package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderCode;
    private String lookupCode;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private ShippingStatus shippingStatus;
    private ReturnStatus returnStatus;
    private PaymentMethod paymentMethod;
    private String ordererName;
    private String ordererEmail;
    private String ordererPhone;
    private String receiverName;
    private String receiverPhone;
    private String shippingAddressLine;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private BigDecimal returnedAmount;
    private BigDecimal finalAmount;
    private String voucherCode;
    private String promotionCode;
    private String shippingCarrier;
    private String trackingCode;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private List<OrderItemResponse> items;
}
