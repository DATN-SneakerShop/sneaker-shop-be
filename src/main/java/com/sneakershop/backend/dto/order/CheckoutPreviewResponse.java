package com.sneakershop.backend.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CheckoutPreviewResponse {
    private Long cartId;
    private Long customerId;
    private String sessionKey;
    private Integer totalItems;
    private Integer selectedItemCount;
    private BigDecimal subtotalAmount;
    private BigDecimal promotionDiscountAmount;
    private BigDecimal voucherDiscountAmount;
    private BigDecimal shippingDiscountAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private String voucherCode;
    private String message;
    private String freeShipVoucherCode;
}