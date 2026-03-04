package com.sneakershop.backend.dto.order;

import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class UpdatePaymentRequest {

    @NotNull
    private PaymentStatus paymentStatus;

    @NotNull
    private PaymentMethod paymentMethod;
}
