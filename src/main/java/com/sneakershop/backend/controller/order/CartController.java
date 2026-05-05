package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.AddToCartRequest;
import com.sneakershop.backend.dto.order.CartResponse;
import com.sneakershop.backend.dto.order.UpdateCartItemQuantityRequest;
import com.sneakershop.backend.dto.order.UpdateCartItemSelectionRequest;
import com.sneakershop.backend.service.order.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCurrentCart(
            Principal principal,
            @RequestHeader(value = "X-Cart-Session-Key", required = false) String sessionKey
    ) {
        return ResponseEntity.ok(cartService.getCurrentCart(getPrincipalName(principal), sessionKey));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addToCart(
            Principal principal,
            @RequestHeader(value = "X-Cart-Session-Key", required = false) String sessionKey,
            @RequestBody AddToCartRequest request
    ) {
        return ResponseEntity.ok(cartService.addToCart(getPrincipalName(principal), sessionKey, request));
    }

    @PutMapping("/items/{itemId}/quantity")
    public ResponseEntity<CartResponse> updateCartItemQuantity(
            Principal principal,
            @RequestHeader(value = "X-Cart-Session-Key", required = false) String sessionKey,
            @PathVariable Long itemId,
            @RequestBody UpdateCartItemQuantityRequest request
    ) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(getPrincipalName(principal), sessionKey, itemId, request));
    }

    @PutMapping("/items/{itemId}/selection")
    public ResponseEntity<CartResponse> updateCartItemSelection(
            Principal principal,
            @RequestHeader(value = "X-Cart-Session-Key", required = false) String sessionKey,
            @PathVariable Long itemId,
            @RequestBody UpdateCartItemSelectionRequest request
    ) {
        return ResponseEntity.ok(cartService.updateCartItemSelection(getPrincipalName(principal), sessionKey, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeCartItem(
            Principal principal,
            @RequestHeader(value = "X-Cart-Session-Key", required = false) String sessionKey,
            @PathVariable Long itemId
    ) {
        return ResponseEntity.ok(cartService.removeCartItem(getPrincipalName(principal), sessionKey, itemId));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<CartResponse> clearCart(
            Principal principal,
            @RequestHeader(value = "X-Cart-Session-Key", required = false) String sessionKey
    ) {
        return ResponseEntity.ok(cartService.clearCart(getPrincipalName(principal), sessionKey));
    }

    private String getPrincipalName(Principal principal) {
        return principal != null ? principal.getName() : null;
    }
}