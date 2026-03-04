package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.*;
import com.sneakershop.backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /* ✅ FIX LỖI 500: Chuyển IllegalArgumentException thành mã lỗi 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleLogicError(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.getProducts(PageRequest.of(page, size));
    }

    @GetMapping("/search")
    public Page<ProductResponse> searchProducts(
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        ProductSearchRequest request = new ProductSearchRequest();
        request.setCategoryIds(categoryIds);
        request.setKeyword(keyword);
        return productService.searchAdvanced(request, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public ProductDetailResponse getDetail(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }
}