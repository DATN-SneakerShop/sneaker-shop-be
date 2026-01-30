package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.ProductRequest;
import com.sneakershop.backend.dto.product.ProductResponse;
import com.sneakershop.backend.entity.product.Category;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.repository.product.CategoryRepository;
import com.sneakershop.backend.repository.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /* ================== LIST (ADMIN / PRODUCT LIST) ================== */
    public Page<ProductResponse> getProducts(Long categoryId, Pageable pageable) {

        Page<Product> page;

        if (categoryId != null) {
            page = productRepository.findByCategory_Id(categoryId, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }

        return page.map(this::toResponse);
    }

    /* ================== CREATE ================== */
    public Product create(ProductRequest request) {

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product p = new Product();
        p.setName(request.getName());
        p.setDescription(request.getDescription());
        p.setCategory(category);

        p.setBrand(request.getBrand());
        p.setGender(request.getGender());
        p.setReleaseType(request.getReleaseType());

        p.setThumbnail(request.getThumbnail());

        // default status
        p.setStatus(
                request.getStatus() != null ? request.getStatus() : "IN_STOCK"
        );

        return productRepository.save(p);
    }

    /* ================== MAP ENTITY → DTO ================== */
    private ProductResponse toResponse(Product p) {

        ProductResponse res = new ProductResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setDescription(p.getDescription());
        res.setStatus(p.getStatus());
        res.setThumbnail(p.getThumbnail());

        // category
        if (p.getCategory() != null) {
            res.setCategoryId(p.getCategory().getId());
            res.setCategoryName(p.getCategory().getName());
        }

        // priceFrom (từ variant)
        if (p.getVariants() == null || p.getVariants().isEmpty()) {
            res.setPriceFrom(BigDecimal.ZERO);
        } else {
            BigDecimal minPrice = p.getVariants()
                    .stream()
                    .map(v -> v.getSalePrice() != null ? v.getSalePrice() : v.getPrice())
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            res.setPriceFrom(minPrice);
        }

        return res;
    }

    /* ================== DETAIL ================== */
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }
}
