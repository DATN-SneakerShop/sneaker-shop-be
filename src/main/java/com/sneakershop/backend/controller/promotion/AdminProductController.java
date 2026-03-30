package com.sneakershop.backend.controller.promotion;

import com.sneakershop.backend.dto.product.ProductSimpleResponse;
import com.sneakershop.backend.dto.product.VariantResponse;
import com.sneakershop.backend.service.product.ProductService;
import com.sneakershop.backend.service.promotion.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/promotion-products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final PromotionService promotionService;

    @GetMapping
    public List<ProductSimpleResponse> getAll(
            @RequestParam(required = false) Long promotionId) {
        if (promotionId != null) {
            // ✅ Đã khớp với ProductService mới
            return productService.getAllForPromotionEdit(promotionId);
        }
        // ✅ Đã khớp với ProductService mới
        return productService.getAll();
    }
    @GetMapping("/check-name")
    public boolean checkName(@RequestParam String name) {
        return promotionService.checkName(name);
    }

    @GetMapping("/{id}/variants")
    public List<VariantResponse> getVariants(@PathVariable Long id) {
        // ✅ Đã khớp với ProductService mới
        return productService.getVariantsByProduct(id);
    }
}