package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.storefront.StorefrontOrderDetailResponse;
import com.sneakershop.backend.dto.order.storefront.StorefrontOrderSummaryResponse;
import com.sneakershop.backend.service.order.StorefrontOrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/storefront/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StorefrontMyOrderController {

    private final StorefrontOrderQueryService storefrontOrderQueryService;

    @GetMapping("/my")
    public ResponseEntity<List<StorefrontOrderSummaryResponse>> getMyOrders(Principal principal) {
        return ResponseEntity.ok(
                storefrontOrderQueryService.getMyOrders(principal != null ? principal.getName() : null)
        );
    }

    @GetMapping("/my/{orderId}")
    public ResponseEntity<StorefrontOrderDetailResponse> getMyOrderDetail(
            Principal principal,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                storefrontOrderQueryService.getMyOrderDetail(principal != null ? principal.getName() : null, orderId)
        );
    }
}