package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
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

    @AuditAction(module = "PRODUCT", action = "CREATE", entity = "Category",
            description = "Đã thêm mới danh mục: #{#request.name}")
    public CategoryResponse create(CategoryRequest request) {
        Category c = new Category();
        c.setName(request.getName());
        c.setDescription(request.getDescription());
        c.setThumbnail(request.getThumbnail());
        categoryRepository.save(c);
        return mapToResponse(c);
    }

    public List<CategoryResponse> getAll() {
        // 🔥 SỬA: Dùng findAllByOrderByIdDesc() để hàng mới luôn ở đầu danh sách
        return categoryRepository.findAllByOrderByIdDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "UPDATE", entity = "Category",
            description = "Đã cập nhật danh mục ID #{#id} thành tên: #{#request.name}")
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setThumbnail(request.getThumbnail());
        return mapToResponse(category);
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "DELETE", entity = "Category",
            description = "Đã xóa danh mục ID #{#id} khỏi hệ thống")
    public void delete(Long id) {
        Category category = categoryRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        if (category.getProducts() != null) {
            for (Product product : category.getProducts()) {
                product.getCategories().remove(category);
            }
        }
        categoryRepository.delete(category);
    }

    private CategoryResponse mapToResponse(Category category) {
        CategoryResponse res = new CategoryResponse();
        res.setId(category.getId());
        res.setName(category.getName());
        res.setDescription(category.getDescription());
        res.setThumbnail(category.getThumbnail());
        return res;
    }
}