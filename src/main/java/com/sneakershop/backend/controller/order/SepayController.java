package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.CheckoutResponse;
import com.sneakershop.backend.dto.order.SepayWebhookRequest;
import com.sneakershop.backend.service.order.SepayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sepay")
@RequiredArgsConstructor
public class SepayController {

    private final SepayWebhookService sepayWebhookService;

    @GetMapping("/payment-info/{orderCode}")
    public ResponseEntity<?> getPaymentInfo(
            @PathVariable String orderCode,
            @RequestParam String lookupCode
    ) {
        return ResponseEntity.ok(sepayWebhookService.getPaymentInfo(orderCode, lookupCode));
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SepayWebhookRequest request,
            HttpServletRequest httpRequest
    ) {
        sepayWebhookService.handleWebhook(authorization, request);

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("message", "Webhook processed");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}