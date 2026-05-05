package com.sneakershop.backend.dto.order.admin;

import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import com.sneakershop.backend.entity.order.enums.ShippingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminOrderSummaryDTO {
    private Long id;
    private String orderCode;
    private String lookupCode;
    private LocalDateTime createdAt;

    private Long customerId;
    private Boolean guestOrder;
    private String ordererName;
    private String ordererEmail;
    private String ordererPhone;

    private String receiverName;
    private String receiverPhone;
    private Integer itemCount;

    private BigDecimal totalAmount;
    private BigDecimal finalAmount;

    private SalesChannel channel;
    private PaymentMethod paymentMethod;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private ShippingStatus shippingStatus;
}
