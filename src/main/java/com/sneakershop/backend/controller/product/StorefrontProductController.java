package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.StorefrontHomeProductResponse;
import com.sneakershop.backend.service.product.StorefrontProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class StorefrontProductController {

    private final StorefrontProductService storefrontProductService;

    @GetMapping("/home")
    public List<StorefrontHomeProductResponse> getHomeProducts() {
        return storefrontProductService.getHomeProducts();
    }
    @GetMapping("/{id}")
    public Object getStorefrontProductDetail(@PathVariable Long id) {
        return storefrontProductService.getProductDetail(id);
    }
}