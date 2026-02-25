package com.sneakershop.backend.controller.product;

import com.sneakershop.backend.dto.product.CategoryRequest;
import com.sneakershop.backend.entity.product.Category;
import com.sneakershop.backend.service.product.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ✅ Thêm category
    @PostMapping
    public Category create(@RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    // ✅ Lấy tất cả category (menu trang chủ)
    @GetMapping
    public List<Category> getAll() {
        return categoryService.getAll();
    }
}
