package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.CategoryRequest;
import com.sneakershop.backend.dto.product.CategoryResponse;
import com.sneakershop.backend.entity.product.Category;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.repository.product.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /* ================= CREATE ================= */
    public CategoryResponse create(CategoryRequest request) {
        Category c = new Category();
        c.setName(request.getName());
        c.setDescription(request.getDescription());
        c.setThumbnail(request.getThumbnail()); // 👈 thêm thumbnail

        categoryRepository.save(c);

        return mapToResponse(c);
    }

    /* ================= GET ALL ================= */
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /* ================= UPDATE ================= */
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Category not found: " + id)
                );

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setThumbnail(request.getThumbnail()); // 👈 thêm thumbnail

        return mapToResponse(category);
    }

    /* ================= DELETE ================= */
    @Transactional
    public void delete(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Category not found: " + id)
                );

        // Gỡ khỏi tất cả product trước khi xóa
        if (category.getProducts() != null) {
            for (Product product : category.getProducts()) {
                product.getCategories().remove(category);
            }
        }

        categoryRepository.delete(category);
    }

    /* ================= MAP ENTITY -> RESPONSE ================= */
    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse res = new CategoryResponse();
        res.setId(category.getId());
        res.setName(category.getName());
        res.setDescription(category.getDescription());
        res.setThumbnail(category.getThumbnail());
        return res;
    }
}
