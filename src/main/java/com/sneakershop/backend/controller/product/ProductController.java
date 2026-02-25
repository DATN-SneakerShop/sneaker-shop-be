package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.ProductRequest;
import com.sneakershop.backend.dto.product.ProductResponse;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ================= CREATE =================
    @PostMapping
    public Product create(@RequestBody ProductRequest request) {
        return productService.create(request);
    }

    // ================= LIST + PAGINATION + FILTER =================
    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId
    ) {
        return productService.getProducts(
                categoryId,
                PageRequest.of(page, size)
        );
    }

    // ================= DETAIL =================
    @GetMapping("/{id}")
    public Product getDetail(@PathVariable Long id) {
        return productService.getById(id);
    }
}
