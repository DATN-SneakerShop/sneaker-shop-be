package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.CategoryRequest;
import com.sneakershop.backend.entity.product.Category;
import com.sneakershop.backend.repository.product.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category create(CategoryRequest request) {
        Category c = new Category();
        c.setName(request.getName());
        c.setDescription(request.getDescription());
        return categoryRepository.save(c);
    }

    public List<Category> getAll() {
        return categoryRepository.findAll();
    }
}
