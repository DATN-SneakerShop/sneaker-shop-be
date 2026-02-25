package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.ProductRequest;
import com.sneakershop.backend.dto.product.ProductResponse;
import com.sneakershop.backend.dto.product.ProductSimpleResponse;
import com.sneakershop.backend.dto.product.VariantResponse;
import com.sneakershop.backend.entity.product.Category;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.repository.product.CategoryRepository;
import com.sneakershop.backend.repository.product.ProductRepository;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductPriceRepository productPriceRepository;

    /* ================== LIST (ADMIN / PRODUCT LIST) ================== */
    public Page<ProductResponse> getProducts(Long categoryId, Pageable pageable) {

        Page<Product> page = (categoryId != null)
                ? productRepository.findByCategory_Id(categoryId, pageable)
                : productRepository.findAll(pageable);

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

        // trạng thái mặc định
        p.setStatus(request.getStatus() != null ? request.getStatus() : "IN_STOCK");

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

        // priceFrom = giá thấp nhất trong các variant (chỉ lấy giá đang active)
        BigDecimal minPrice = p.getVariants() == null || p.getVariants().isEmpty()
                ? BigDecimal.ZERO
                : p.getVariants().stream()
                .map(v -> productPriceRepository
                        .findByVariant_IdAndEndDateIsNull(v.getId())
                        .map(pp -> pp.getPrice())
                        .orElse(null))
                .filter(gia -> gia != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        res.setPriceFrom(minPrice);

        return res;
    }

    /* ================== DETAIL ================== */
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }
    public List<VariantResponse> getVariantsByProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        return product.getVariants().stream().map(v -> {
            VariantResponse vr = new VariantResponse();
            vr.setVariantId(v.getId());
            vr.setSku(v.getSku());
            vr.setSize(v.getSize());
            vr.setColorway(v.getColorway());
            vr.setStock(v.getStock());

            // lấy GIÁ ĐÚNG
            vr.setPrice(
                    productPriceRepository
                            .findByVariant_IdAndEndDateIsNull(v.getId())
                            .map(pp -> pp.getPrice())
                            .orElse(BigDecimal.ZERO)
            );

            vr.setStock(v.getStock());
            return vr;
        }).toList();
    }
    // CREATE
    public List<ProductSimpleResponse> getAll() {
        return productRepository.findAllSimpleWithVariantCount();
    }

    // EDIT
    public List<ProductSimpleResponse> getAllForPromotionEdit(Long promotionId) {
        return productRepository.findAllSimpleForPromotionEdit(promotionId);
    }

}
