package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.CategoryRequest;
import com.sneakershop.backend.dto.product.CategoryResponse;
import com.sneakershop.backend.dto.product.ProductResponse;
import com.sneakershop.backend.service.product.CategoryService;
import com.sneakershop.backend.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ProductService productService;

    // ✅ Thêm category
    @PostMapping
    public CategoryResponse create(@RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    // ✅ Lấy tất cả category
    @GetMapping
    public List<CategoryResponse> getAll() {
        return categoryService.getAll();
    }

    // ✅ Lấy sản phẩm theo category
    @GetMapping("/{id}/products")
    public Page<ProductResponse> getProductsByCategory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.searchProducts(
                List.of(id),
                null,
                page,
                size
        );
    }

    // ✅ Cập nhật category
    @PutMapping("/{id}")
    public CategoryResponse update(
            @PathVariable Long id,
            @RequestBody CategoryRequest request
    ) {
        return categoryService.update(id, request);
    }

    // ✅ Xóa category
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
