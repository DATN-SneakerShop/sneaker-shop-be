package com.sneakershop.backend.controller.pricing;

import com.sneakershop.backend.dto.pricing.ProductCardDTO;
import com.sneakershop.backend.service.pricing.ProductPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pricing/products")
@RequiredArgsConstructor
public class ProductPricingController {

    private final ProductPricingService productPricingService;

    @GetMapping
    public List<ProductCardDTO> getProductCards(){
        return productPricingService.getProductCards();
    }
}
