package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.*;
import com.sneakershop.backend.entity.product.*;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductPriceRepository productPriceRepository;

    private static final Map<String, String> STATUS_LABEL = Map.of(
            "Còn hàng", "Còn hàng",
            "Hết hàng", "Hết hàng",
            "Đặt trước", "Đặt trước",
            "Ngừng bán", "Ngừng bán"
    );

    // ✅ KHỚP: image_8c4b09.jpg - Controller gọi getProducts(PageRequest)
    public Page<ProductResponse> getProducts(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id").descending());
        }
        return productRepository.findAll(pageable).map(this::toListResponse);
    }

    // ✅ KHỚP: image_8c4b27.jpg - Controller gọi searchProducts(...)
    public Page<ProductResponse> searchProducts(List<Long> categoryIds, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        long categoryCount = (categoryIds != null) ? categoryIds.size() : 0;
        return productRepository.searchProducts(categoryIds, keyword, categoryCount, pageable).map(this::toListResponse);
    }

    // ✅ KHỚP: image_973b71.jpg - Đã sửa toResponse thành toListResponse
    public Page<ProductResponse> searchAdvanced(ProductSearchRequest request, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id").descending());
        }
        return productRepository.findAll(ProductSpecification.build(request), pageable).map(this::toListResponse);
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "CREATE", entity = "Product",
            description = "Thêm SP mới | Tên: #{#request.name} | SKU: #{#request.sku}")
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setBrand(request.getBrand());
        product.setStatus(request.getStatus() != null ? request.getStatus() : "Còn hàng");
        product.setThumbnail(request.getThumbnail());
        product.setCreatedAt(LocalDateTime.now());
        product.setCategories(categoryRepository.findAllById(request.getCategoryIds()));

        if (request.getVariants() != null) {
            List<ProductVariant> variants = request.getVariants().stream().map(vReq -> {
                ProductVariant v = new ProductVariant();
                v.setProduct(product);
                v.setSize(vReq.getSize());
                v.setColorway(vReq.getColorway());
                v.setPrice(vReq.getPrice() != null ? vReq.getPrice() : BigDecimal.ZERO);
                v.setStock(vReq.getStock());
                v.setSku(product.getSku() + "-" + vReq.getSize());
                return v;
            }).collect(Collectors.toList());
            product.setVariants(variants);
        }
        return toListResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getById(Long id) {
        Product p = productRepository.findDetailById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        return toDetailResponse(p);
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "UPDATE", entity = "Product",
            description = "Sửa SP ID #{#id}")
    public ProductResponse update(Long id, ProductRequest request) {
        Product p = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        p.setName(request.getName());
        p.setBrand(request.getBrand());
        p.setThumbnail(request.getThumbnail());
        p.setCategories(categoryRepository.findAllById(request.getCategoryIds()));
        return toListResponse(productRepository.save(p));
    }

    @AuditAction(module = "PRODUCT", action = "DELETE", entity = "Product",
            description = "Xóa SP ID: #{#id}")
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    public List<ProductSimpleResponse> getAll() {
        return productRepository.findAllSimpleWithVariantCount();
    }

    public List<ProductSimpleResponse> getAllForPromotionEdit(Long promotionId) {
        return productRepository.findAllSimpleForPromotionEdit(promotionId);
    }

    public List<VariantResponse> getVariantsByProduct(Long productId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found"));
        return product.getVariants().stream().map(v -> {
            VariantResponse vr = new VariantResponse();
            vr.setVariantId(v.getId());
            vr.setSku(v.getSku());
            vr.setSize(v.getSize());
            vr.setColorway(v.getColorway());
            vr.setStock(v.getStock());
            BigDecimal activePrice = productPriceRepository.findByVariant_IdAndEndDateIsNull(v.getId())
                    .map(ProductPrice::getPrice).orElse(BigDecimal.ZERO);
            vr.setPrice(activePrice);
            return vr;
        }).collect(Collectors.toList());
    }

    private ProductResponse toListResponse(Product p) {
        ProductResponse res = new ProductResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setSku(p.getSku());
        res.setStatus(STATUS_LABEL.getOrDefault(p.getStatus(), p.getStatus()));
        res.setThumbnail(p.getThumbnail());
        if (p.getCategories() != null) {
            res.setCategoryNames(p.getCategories().stream().map(Category::getName).collect(Collectors.toList()));
        }
        return res;
    }

    private ProductDetailResponse toDetailResponse(Product p) {
        ProductDetailResponse res = new ProductDetailResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setSku(p.getSku());
        res.setThumbnail(p.getThumbnail());
        if (p.getVariants() != null) {
            res.setVariants(p.getVariants().stream().map(v -> {
                ProductVariantResponse vRes = new ProductVariantResponse();
                vRes.setId(v.getId());
                vRes.setSku(v.getSku());
                vRes.setSize(v.getSize());
                BigDecimal activePrice = productPriceRepository.findByVariant_IdAndEndDateIsNull(v.getId())
                        .map(ProductPrice::getPrice).orElse(BigDecimal.ZERO);
                vRes.setPrice(activePrice);
                vRes.setStock(v.getStock());
                return vRes;
            }).collect(Collectors.toList()));
        }
        return res;
    }
}