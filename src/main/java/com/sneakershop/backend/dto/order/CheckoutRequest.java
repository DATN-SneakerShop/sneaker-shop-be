package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CheckoutRequest {
    private Long cartId;
    private String sessionKey;
    private Long customerId;

    @NotNull
    private PaymentMethod paymentMethod;

    private String voucherCode;
    private String freeShipVoucherCode;
    private String ordererName;
    private String ordererEmail;
    private String ordererPhone;

    @NotBlank
    private String receiverName;
    @NotBlank
    private String receiverPhone;
    @NotBlank
    private String shippingProvince;
    @NotBlank
    private String shippingDistrict;
    @NotBlank
    private String shippingWard;
    @NotBlank
    private String shippingDetailAddress;

    private String addressLabel;
    private String note;
}
