package com.sneakershop.backend.controller.pricing;
import com.sneakershop.backend.dto.pricing.PriceResultDTO;
import com.sneakershop.backend.dto.pricing.PricingCalculateRequest;
import com.sneakershop.backend.service.pricing.ProductPricingPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final ProductPricingPromotionService pricingService;

    @PostMapping("/calculate")
    public PriceResultDTO calculate(
            @RequestBody PricingCalculateRequest request
    ) {
        return pricingService.calculateFinalPrice(
                request.getVariantId(),
                request.getQuantity()
        );
    }}


