package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.VariantRequest;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.service.product.ProductVariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService variantService;

    /* ================== CREATE ================== */
    @PostMapping
    public ProductVariant create(
            @PathVariable Long productId,
            @RequestBody VariantRequest request
    ) {
        return variantService.create(productId, request);
    }

    /* ================== UPDATE ================== */
    @PutMapping("/{variantId}")
    public ProductVariant update(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @RequestBody VariantRequest request
    ) {
        return variantService.update(productId, variantId, request);
    }

    /* ================== DELETE ================== */
    @DeleteMapping("/{variantId}")
    public void delete(
            @PathVariable Long productId,
            @PathVariable Long variantId
    ) {
        variantService.delete(productId, variantId);
    }
}
