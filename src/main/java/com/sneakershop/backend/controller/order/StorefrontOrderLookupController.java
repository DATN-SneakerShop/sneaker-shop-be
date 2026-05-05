package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.storefront.StorefrontOrderDetailResponse;
import com.sneakershop.backend.service.order.StorefrontOrderLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storefront/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StorefrontOrderLookupController {

    private final StorefrontOrderLookupService storefrontOrderLookupService;

    @GetMapping("/lookup")
    public ResponseEntity<StorefrontOrderDetailResponse> lookup(
            @RequestParam String keyword
    ) {
        return ResponseEntity.ok(storefrontOrderLookupService.lookup(keyword));
    }
}