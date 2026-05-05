package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.CheckoutPreviewRequest;
import com.sneakershop.backend.dto.order.CheckoutPreviewResponse;
import com.sneakershop.backend.dto.order.CheckoutRequest;
import com.sneakershop.backend.dto.order.CheckoutResponse;
import com.sneakershop.backend.service.order.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CheckoutController {
    private final CheckoutService checkoutService;

    @PostMapping("/preview")
    public ResponseEntity<CheckoutPreviewResponse> preview(@RequestBody CheckoutPreviewRequest request) {
        return ResponseEntity.ok(checkoutService.preview(request));
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(checkoutService.checkout(request));
    }
}