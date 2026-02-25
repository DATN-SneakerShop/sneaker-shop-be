package com.sneakershop.backend.controller.pricing;

import com.sneakershop.backend.AuditAction;
import com.sneakershop.backend.dto.pricing.PriceBoardDTO;
import com.sneakershop.backend.dto.pricing.PriceHistoryDTO;
import com.sneakershop.backend.dto.pricing.PriceRequest;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.service.pricing.ProductPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class ProductPriceController {

    private final ProductPriceService productPriceService;

    // ✅ Bảng giá hiện tại
    @GetMapping("/board")
    public List<PriceBoardDTO> getPriceBoard() {
        return productPriceService.getPriceBoard();
    }

    // ✅ Lịch sử giá theo variant
    @GetMapping("/variant/{variantId}")
    public List<PriceHistoryDTO> getPricesByVariant(
            @PathVariable Long variantId
    ) {
        return productPriceService.getPriceHistoryByVariant(variantId);
    }


    @PostMapping("/variant/{variantId}")
    @AuditAction(
            module = "PRICING",
            action = "CREATE_PRICE",
            entity = "PRODUCT_PRICE"
    )
    public ProductPrice createPrice(
            @PathVariable Long variantId,
            @RequestBody PriceRequest request
    ) {
        return productPriceService.updatePrice(variantId, request);
    }


    // ❌ Xóa giá (chỉ xóa giá lịch sử)
    @DeleteMapping("/{id}")
    @AuditAction(
            module = "PRICING",
            action = "DELETE_PRICE",
            entity = "PRODUCT_PRICE"
    )
    public void deletePrice(@PathVariable Long id) {
        productPriceService.deletePrice(id);
    }
}
