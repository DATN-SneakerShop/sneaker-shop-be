package com.sneakershop.backend.dto.order.admin;

import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.ShippingStatus;
import lombok.Data;

@Data
public class AdminOrderUpdateRequest {
    private PaymentStatus paymentStatus;
    private ShippingStatus shippingStatus;
    private String shippingCarrier;
    private String trackingCode;
    private String deliveryFailReason;
    private String note;
}
