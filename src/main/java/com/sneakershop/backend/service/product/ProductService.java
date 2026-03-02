package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.*;
import com.sneakershop.backend.entity.product.*;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.product.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private static final String IMAGE_PREFIX = "/uploads/";
    private static final Map<String, String> STATUS_LABEL = Map.of(
            "Còn hàng", "Còn hàng",
            "Hết hàng", "Hết hàng",
            "Đặt trước", "Đặt trước",
            "Ngừng bán", "Ngừng bán"
    );
    /* =====================================================
       CREATE PRODUCT + VARIANTS
    ===================================================== */
    @Transactional
    public ProductResponse create(ProductRequest request) {

        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("Product SKU already exists");
        }

        /* ===== CREATE PRODUCT ===== */
        Product product = new Product();
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setBrand(request.getBrand());
        product.setModel(request.getModel());
        product.setReleaseYear(request.getReleaseYear());
        product.setGender(request.getGender());
        product.setReleaseType(request.getReleaseType());
        product.setStatus(request.getStatus());
        product.setMaterial(request.getMaterial());
        product.setLimited(Boolean.TRUE.equals(request.getLimited()));
        product.setDescription(request.getDescription());
        product.setThumbnail(request.getThumbnail());
        product.setCreatedAt(LocalDateTime.now());

        /* ===== CATEGORY ===== */
        product.setCategories(
                categoryRepository.findAllById(request.getCategoryIds())
        );
        /* ===== IMAGES ===== */
        List<ProductImage> images = new ArrayList<>();

        for (ProductImageRequest imgReq : request.getImages()) {
            ProductImage img = new ProductImage();
            img.setImageUrl(imgReq.getImageUrl());
            img.setThumbnail(imgReq.isThumbnail());
            img.setProduct(product); // 🔥 DÒNG QUYẾT ĐỊNH
            images.add(img);
        }

// gắn vào product để cascade save
        product.setImages(images);
        productRepository.save(product);
        /* ===== VARIANTS ===== */
        List<ProductVariant> variants = new ArrayList<>();

        for (VariantRequest v : request.getVariants()) {

            if (v.getSize() == null || v.getSizeType() == null || v.getColorway() == null) {
                throw new IllegalArgumentException(
                        "Variant size, sizeType, colorway are required"
                );
            }

            if (productVariantRepository
                    .existsByProduct_IdAndSizeAndSizeTypeAndColorway(
                            product.getId(),
                            v.getSize(),
                            v.getSizeType(),
                            v.getColorway()
                    )) {
                throw new IllegalArgumentException(
                        "Duplicate variant: size + sizeType + colorway"
                );
            }

            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(v.getSize());
            variant.setSizeType(v.getSizeType());
            variant.setColorway(v.getColorway());
            if (v.getPrice() == null) {
                throw new IllegalArgumentException("Variant price is required");
            }
            variant.setPrice(v.getPrice());
            variant.setSalePrice(v.getSalePrice());
            variant.setStock(v.getStock());
            variant.setStatus(
                    v.getStock() > 0 ? "Còn_hàng" : "Hết_Hàng"
            );

            String sku = generateVariantSku(product, v);

            if (productVariantRepository.existsBySku(sku)) {
                throw new IllegalArgumentException("Variant SKU already exists: " + sku);
            }

            variant.setSku(sku);
            variants.add(variant);
        }

        productVariantRepository.saveAll(variants);
        product.setVariants(variants);

        return toListResponse(product);
    }

    /* =====================================================
       LIST DEFAULT
    ===================================================== */
    public Page<ProductResponse> getProducts(Pageable pageable) {
        return productRepository
                .findAll(pageable)
                .map(this::toListResponse);
    }
/* =====================================================
   SEARCH MULTI CATEGORY + KEYWORD
===================================================== */
    public Page<ProductResponse> searchProducts(
            List<Long> categoryIds,
            String keyword,
            int page,
            int size
    ) {
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }

        Pageable pageable = PageRequest.of(page, size);

        // Nếu không lọc category → lấy tất cả
        if (categoryIds == null || categoryIds.isEmpty()) {
            return productRepository
                    .findAll(pageable)
                    .map(this::toListResponse);
        }

        Page<Product> productPage =
                productRepository.searchProducts(
                        categoryIds,
                        keyword,
                        categoryIds.size(),   // 👈 thêm dòng này
                        pageable
                );

        return productPage.map(this::toListResponse);
    }
    /* =====================================================
       SEARCH ADVANCED
    ===================================================== */
    public Page<ProductResponse> searchAdvanced(
            ProductSearchRequest request,
            Pageable pageable
    ) {

        String sort = request.getSort();
        String sortPrice = request.getSortPrice();

        // 🔥 1. ƯU TIÊN BÁN CHẠY
        if ("best_seller".equalsIgnoreCase(sort)) {
            return productRepository
                    .findBestSellingProducts(pageable)
                    .map(this::toListResponse);
        }

        // 🔥 2. MỚI NHẤT
        if ("newest".equalsIgnoreCase(sort)) {
            Pageable p = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("createdAt").descending()
            );

            return productRepository
                    .findAll(ProductSpecification.build(request), p)
                    .map(this::toListResponse);
        }

        // 🔥 3. SORT GIÁ (GIỮ NGUYÊN LOGIC CŨ)
        if ("asc".equalsIgnoreCase(sortPrice)) {
            Pageable p = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("id").ascending()
            );

            return productRepository
                    .findAll(ProductSpecification.build(request), p)
                    .map(this::toListResponse);
        }

        if ("desc".equalsIgnoreCase(sortPrice)) {
            Pageable p = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("id").descending()
            );

            return productRepository
                    .findAll(ProductSpecification.build(request), p)
                    .map(this::toListResponse);
        }

        // 🔥 DEFAULT
        return productRepository
                .findAll(ProductSpecification.build(request), pageable)
                .map(this::toListResponse);
    }
    /* =====================================================
       DETAIL
    ===================================================== */
    @Transactional(readOnly = true)
    public ProductDetailResponse getById(Long id) {

        Product product = productRepository.findDetailById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Product not found: " + id)
                );

        return toDetailResponse(product);
    }

    /* =====================================================
       UPDATE PRODUCT + VARIANTS
    ===================================================== */
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Product not found: " + id)
                );

        /* ===== BASIC INFO ===== */
        product.setName(request.getName());
        product.setBrand(request.getBrand());
        product.setModel(request.getModel());
        product.setReleaseYear(request.getReleaseYear());
        product.setGender(request.getGender());
        product.setReleaseType(request.getReleaseType());
        product.setStatus(request.getStatus());
        product.setMaterial(request.getMaterial());
        product.setLimited(Boolean.TRUE.equals(request.getLimited()));
        product.setDescription(request.getDescription());
        product.setThumbnail(request.getThumbnail());

        /* ===== CATEGORY ===== */
        product.setCategories(
                categoryRepository.findAllById(request.getCategoryIds())
        );

        /* ===== VARIANTS ===== */
        List<ProductVariant> existingVariants = product.getVariants();
        List<Long> requestVariantIds = request.getVariants()
                .stream()
                .map(VariantRequest::getId)
                .filter(idVar -> idVar != null)
                .toList();

        // ❌ remove variant bị xóa
        existingVariants.removeIf(
                v -> v.getId() != null && !requestVariantIds.contains(v.getId())
        );

        for (VariantRequest v : request.getVariants()) {

            ProductVariant variant;

            if (v.getId() != null) {
                variant = existingVariants.stream()
                        .filter(ev -> ev.getId().equals(v.getId()))
                        .findFirst()
                        .orElseThrow(() ->
                                new EntityNotFoundException("Variant not found: " + v.getId())
                        );
            } else {
                variant = new ProductVariant();
                variant.setProduct(product);
                existingVariants.add(variant);
            }

            variant.setSize(v.getSize());
            variant.setSizeType(v.getSizeType());
            variant.setColorway(v.getColorway());
            variant.setPrice(v.getPrice());
            variant.setSalePrice(v.getSalePrice());
            variant.setStock(v.getStock());
            variant.setStatus(
                    v.getStock() > 0 ? "CÒN_HÀNG" : "HẾT_HÀNG"
            );
        }

        productRepository.save(product);
        return toListResponse(product);
    }

    /* =====================================================
       DELETE PRODUCT
    ===================================================== */
    @Transactional
    public void delete(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Product not found: " + id)
                );

        productRepository.delete(product);
    }

    /* =====================================================
       SKU GENERATOR
    ===================================================== */
    private String generateVariantSku(Product product, VariantRequest v) {
        return String.format(
                "%s-%s-%s-%s%s",
                safe(product.getModel()),
                safe(v.getColorway()).replace("/", "").replace(" ", ""),
                product.getReleaseYear() != null ? product.getReleaseYear() : "NA",
                v.getSize(),
                v.getSizeType()
        ).toUpperCase();
    }

    private String safe(String value) {
        return value == null ? "NA" : value;
    }

    /* =====================================================
       LIST RESPONSE MAPPER
    ===================================================== */
    private ProductResponse toListResponse(Product product) {

        ProductResponse res = new ProductResponse();
        res.setId(product.getId());
        res.setName(product.getName());
        res.setSku(product.getSku());
        res.setDescription(product.getDescription());
        res.setStatus(
                STATUS_LABEL.getOrDefault(
                        product.getStatus(),
                        product.getStatus()
                )
        );
        res.setBrand(product.getBrand());
        res.setGender(product.getGender());
        res.setReleaseType(product.getReleaseType());
        res.setThumbnail(product.getThumbnail());

        if (product.getCategories() != null) {
            res.setCategoryIds(
                    product.getCategories()
                            .stream()
                            .map(Category::getId)
                            .collect(Collectors.toList())
            );

            res.setCategoryNames(
                    product.getCategories()
                            .stream()
                            .map(Category::getName)
                            .collect(Collectors.toList())
            );
        }
        return res;
    }

    /* =====================================================
       DETAIL RESPONSE MAPPER
    ===================================================== */
    private ProductDetailResponse toDetailResponse(Product product) {

        ProductDetailResponse res = new ProductDetailResponse();

        res.setId(product.getId());
        res.setName(product.getName());
        res.setSku(product.getSku());
        res.setBrand(product.getBrand());
        res.setModel(product.getModel());
        res.setReleaseYear(product.getReleaseYear());
        res.setGender(product.getGender());
        res.setReleaseType(product.getReleaseType());
        res.setStatus(product.getStatus());
        res.setMaterial(product.getMaterial());
        res.setLimited(product.getLimited());
        res.setDescription(product.getDescription());

        res.setCategoryIds(
                product.getCategories()
                        .stream()
                        .map(Category::getId)
                        .collect(Collectors.toList())
        );

        res.setCategoryNames(
                product.getCategories()
                        .stream()
                        .map(Category::getName)
                        .collect(Collectors.toList())
        );

        res.setImages(
                product.getImages()
                        .stream()
                        .map(img -> {
                            ProductImageResponse i = new ProductImageResponse();
                            i.setId(img.getId());
                            i.setUrl(img.getImageUrl());
                            i.setThumbnail(img.isThumbnail());
                            return i;
                        })
                        .collect(Collectors.toList())
        );

        res.setVariants(
                product.getVariants()
                        .stream()
                        .map(v -> {
                            ProductVariantResponse vr = new ProductVariantResponse();
                            vr.setId(v.getId());
                            vr.setSku(v.getSku());
                            vr.setSize(v.getSize());
                            vr.setSizeType(v.getSizeType());
                            vr.setColorway(v.getColorway());
                            vr.setStock(v.getStock());
                            vr.setStatus(v.getStatus());
                            vr.setPrice(v.getPrice());
                            vr.setSalePrice(v.getSalePrice());
                            return vr;
                        })
                        .collect(Collectors.toList())
        );

        return res;
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

            vr.setPrice(
                    productPriceRepository
                            .findByVariant_IdAndEndDateIsNull(v.getId())
                            .map(pp -> pp.getPrice())
                            .orElse(BigDecimal.ZERO)
            );

            return vr;
        }).toList();
    }
    public List<ProductSimpleResponse> getAll() {
        return productRepository.findAllSimpleWithVariantCount();
    }
    public List<ProductSimpleResponse> getAllForPromotionEdit(Long promotionId) {
        return productRepository.findAllSimpleForPromotionEdit(promotionId);
    }
}
