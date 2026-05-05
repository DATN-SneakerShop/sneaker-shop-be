package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.PaymentCallbackRequest;
import com.sneakershop.backend.service.order.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/callback/{provider}")
    public ResponseEntity<String> callbackGet(@PathVariable String provider,
                                              @RequestParam(required = false) String orderCode,
                                              @RequestParam(required = false) String transactionRef,
                                              @RequestParam(required = false) String providerTransactionId,
                                              @RequestParam(required = false) String responseCode,
                                              @RequestParam(required = false) String message,
                                              @RequestParam(required = false) String signature,
                                              @RequestParam(required = false) String amount,
                                              @RequestParam(required = false) String success) {
        PaymentCallbackRequest request = new PaymentCallbackRequest();
        request.setProvider(provider);
        request.setOrderCode(orderCode);
        request.setTransactionRef(transactionRef);
        request.setProviderTransactionId(providerTransactionId);
        request.setResponseCode(responseCode);
        request.setMessage(message);
        request.setSignature(signature);
        request.setRawPayload("GET_CALLBACK");
        request.setSuccess("true".equalsIgnoreCase(success) || "00".equals(responseCode));
        if (amount != null && !amount.isBlank()) request.setAmount(new BigDecimal(amount));
        paymentService.handlePaymentCallback(request);
        return ResponseEntity.ok("Callback processed");
    }

    @PostMapping("/callback/{provider}")
    public ResponseEntity<String> callbackPost(@PathVariable String provider, @RequestBody PaymentCallbackRequest request) {
        request.setProvider(provider);
        paymentService.handlePaymentCallback(request);
        return ResponseEntity.ok("Callback processed");
    }

    @PostMapping("/bank-transfer/{orderId}/confirm")
    public ResponseEntity<String> confirmBankTransfer(@PathVariable Long orderId) {
        paymentService.confirmBankTransfer(orderId);
        return ResponseEntity.ok("Bank transfer confirmed");
    }
}
