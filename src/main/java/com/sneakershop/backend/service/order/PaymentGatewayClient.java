package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.PaymentCallbackRequest;
import com.sneakershop.backend.dto.order.PaymentInitResponse;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.PaymentTransaction;

public interface PaymentGatewayClient {
    boolean supports(String providerOrMethod);
    PaymentInitResponse createPaymentUrl(Order order, PaymentTransaction transaction);
    boolean verifyCallback(PaymentCallbackRequest request);
}
