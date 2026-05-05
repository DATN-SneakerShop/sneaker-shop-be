package com.sneakershop.backend.dto.order;

import lombok.Data;

@Data
public class CheckoutPreviewRequest {
    private Long cartId;
    private String sessionKey;
    private Long customerId;
    private String voucherCode;
    private String freeShipVoucherCode;
    private String ShippingProvince;
}