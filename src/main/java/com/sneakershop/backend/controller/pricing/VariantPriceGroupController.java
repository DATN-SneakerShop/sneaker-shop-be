package com.sneakershop.backend.controller.pricing;


import com.sneakershop.backend.dto.pricing.UpdatePriceGroupRequest;

import com.sneakershop.backend.dto.pricing.CreatePriceGroupRequest;
import com.sneakershop.backend.service.pricing.PricingCalculationService;
import com.sneakershop.backend.service.pricing.VariantPriceGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
@RestController
@RequestMapping("/api/pricing/price-group")
@RequiredArgsConstructor
public class VariantPriceGroupController {

    private final VariantPriceGroupService variantPriceGroupService;
    private final PricingCalculationService pricingCalculationService;

    @GetMapping("/list")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(
                variantPriceGroupService.getAll()
        );
    }

    @PostMapping
    public ResponseEntity<?> savePriceGroup(
            @RequestBody CreatePriceGroupRequest request) {

        variantPriceGroupService.savePriceGroup(
                request.getVariantId(),
                request.getLoaiKhach(),
                request.getPrice()
        );

        return ResponseEntity.ok("Created successfully");
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<?> getPrice(
            @PathVariable Long variantId,
            @RequestParam String loaiKhach) {

        BigDecimal price = variantPriceGroupService
                .getPriceByCustomerType(variantId, loaiKhach);

        return ResponseEntity.ok(price);
    }

    @PutMapping
    public ResponseEntity<?> updatePriceGroup(
            @RequestBody UpdatePriceGroupRequest request) {

        return ResponseEntity.ok(
                variantPriceGroupService.updatePriceGroup(
                        request.getVariantId(),
                        request.getLoaiKhach(),
                        request.getPrice()
                )
        );
    }

    @GetMapping("/{variantId}/final-price")
    public ResponseEntity<?> getFinalPrice(
            @PathVariable Long variantId,
            @RequestParam String loaiKhach) {

        BigDecimal finalPrice = pricingCalculationService
                .calculateFinalPrice(variantId, loaiKhach);

        return ResponseEntity.ok(finalPrice);
    }

    @GetMapping("/final-price-board")
    public ResponseEntity<?> getFinalPriceBoard(
            @RequestParam String loaiKhach) {

        return ResponseEntity.ok(
                pricingCalculationService.calculateFinalPriceBoard(loaiKhach)
        );
    }

}