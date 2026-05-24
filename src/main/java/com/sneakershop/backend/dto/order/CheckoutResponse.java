package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.ShippingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CheckoutResponse {
    private Long orderId;
    private String orderCode;
    private String lookupCode;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private ShippingStatus shippingStatus;
    private PaymentMethod paymentMethod;

    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal vipDiscountAmount;
    private String customerRankName;
    private Integer customerRankDiscountPercent;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;

    private String paymentUrl;
    private String message;

    // SePay / Bank transfer
    private String paymentCode;
    private String bankCode;
    private String bankName;
    private String bankAccountNo;
    private String bankAccountName;
    private String transferContent;
    private String qrImageUrl;
}