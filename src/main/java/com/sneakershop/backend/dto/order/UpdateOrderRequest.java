package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateOrderRequest {
    private SalesChannel channel;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    private BigDecimal shippingFee;
    private BigDecimal discountAmount;

    private String note;
}
