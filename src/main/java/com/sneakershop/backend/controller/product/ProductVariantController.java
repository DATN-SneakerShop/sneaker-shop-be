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

    // ✅ Thêm size / SKU cho sản phẩm
    @PostMapping
    public ProductVariant create(
            @PathVariable Long productId,
            @RequestBody VariantRequest request
    ) {
        return variantService.create(productId, request);
    }
}
