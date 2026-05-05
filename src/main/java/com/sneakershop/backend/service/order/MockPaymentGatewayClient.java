package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.PaymentCallbackRequest;
import com.sneakershop.backend.dto.order.PaymentInitResponse;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.PaymentTransaction;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentGatewayClient implements PaymentGatewayClient {
    @Override
    public boolean supports(String providerOrMethod) {
        if (providerOrMethod == null) return false;
        String v = providerOrMethod.trim().toUpperCase();
        return "VNPAY".equals(v) || "MOMO".equals(v);
    }

    @Override
    public PaymentInitResponse createPaymentUrl(Order order, PaymentTransaction transaction) {
        String provider = order.getPaymentMethod().name();
        String paymentUrl = "https://mock-pay.local/pay?orderCode=" + order.getOrderCode() + "&ref=" + transaction.getIdempotencyKey();
        return PaymentInitResponse.builder().provider(provider).paymentUrl(paymentUrl).transactionRef(transaction.getIdempotencyKey()).build();
    }

    @Override
    public boolean verifyCallback(PaymentCallbackRequest request) {
        return request.getSignature() != null && !request.getSignature().isBlank();
    }
}
