package com.sneakershop.backend.service.product;

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

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new IllegalArgumentException("Product SKU already exists");
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setBrand(request.getBrand());
        product.setModel(request.getModel());
        product.setReleaseYear(request.getReleaseYear());
        product.setGender(request.getGender());
        product.setStatus(request.getStatus() != null ? request.getStatus() : "Còn hàng");
        product.setThumbnail(request.getThumbnail());
        product.setCreatedAt(LocalDateTime.now());

        product.setDescription(request.getDescription());
        product.setMaterial(request.getMaterial());
        product.setReleaseType(request.getReleaseType());
        product.setLimited(Boolean.TRUE.equals(request.getLimited()));

        product.setCategories(categoryRepository.findAllById(request.getCategoryIds()));

        List<ProductImage> images = request.getImages().stream().map(imgReq -> {
            ProductImage img = new ProductImage();
            img.setImageUrl(imgReq.getImageUrl());
            img.setThumbnail(imgReq.isThumbnail());
            img.setProduct(product);
            return img;
        }).collect(Collectors.toList());
        product.setImages(images);

        List<ProductVariant> variants = request.getVariants().stream().map(vReq -> {
            ProductVariant v = new ProductVariant();
            v.setProduct(product);
            v.setSize(vReq.getSize());
            v.setSizeType(vReq.getSizeType());
            v.setColorway(vReq.getColorway());

            // 🔥 NẾU FE KHÔNG GỬI GIÁ -> GÁN MẶC ĐỊNH BẰNG 0 ĐỂ CHỜ SET GIÁ SAU
            v.setPrice(vReq.getPrice() != null ? vReq.getPrice() : BigDecimal.ZERO);

            v.setStock(vReq.getStock());
            v.setStatus(vReq.getStock() > 0 ? "Còn hàng" : "Hết hàng");
            v.setSku(generateVariantSku(product, vReq));
            return v;
        }).collect(Collectors.toList());
        product.setVariants(variants);

        return toListResponse(productRepository.save(product));
    }

    public Page<ProductResponse> searchProducts(List<Long> categoryIds, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.searchProducts(categoryIds, keyword, (long) categoryIds.size(), pageable).map(this::toListResponse);
    }

    public Page<ProductResponse> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toListResponse);
    }

    public Page<ProductResponse> searchAdvanced(ProductSearchRequest request, Pageable pageable) {
        return productRepository.findAll(ProductSpecification.build(request), pageable).map(this::toListResponse);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getById(Long id) {
        Product p = productRepository.findDetailById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        return toDetailResponse(p);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product p = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        p.setName(request.getName());
        p.setBrand(request.getBrand());
        p.setThumbnail(request.getThumbnail());
        p.setCategories(categoryRepository.findAllById(request.getCategoryIds()));

        p.setModel(request.getModel());
        p.setReleaseYear(request.getReleaseYear());
        p.setGender(request.getGender());
        p.setStatus(request.getStatus());
        p.setDescription(request.getDescription());
        p.setMaterial(request.getMaterial());
        p.setReleaseType(request.getReleaseType());
        p.setLimited(Boolean.TRUE.equals(request.getLimited()));

        return toListResponse(productRepository.save(p));
    }

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

            // Lấy giá trị đang áp dụng từ bảng ProductPrice
            BigDecimal activePrice = productPriceRepository.findByVariant_IdAndEndDateIsNull(v.getId())
                    .map(ProductPrice::getPrice)
                    .orElse(v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO);
            vr.setPrice(activePrice);

            return vr;
        }).collect(Collectors.toList());
    }

    private String generateVariantSku(Product product, VariantRequest v) {
        return (product.getModel() + "-" + v.getColorway() + "-" + v.getSize()).toUpperCase().replace(" ", "");
    }

    private ProductResponse toListResponse(Product p) {
        ProductResponse res = new ProductResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setSku(p.getSku());
        res.setStatus(STATUS_LABEL.getOrDefault(p.getStatus(), p.getStatus()));
        res.setThumbnail(p.getThumbnail());
        return res;
    }

    private ProductDetailResponse toDetailResponse(Product p) {
        ProductDetailResponse res = new ProductDetailResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setSku(p.getSku());
        res.setBrand(p.getBrand());
        res.setModel(p.getModel());
        res.setStatus(p.getStatus());
        res.setThumbnail(p.getThumbnail());

        res.setReleaseYear(p.getReleaseYear());
        res.setGender(p.getGender());
        res.setReleaseType(p.getReleaseType());
        res.setMaterial(p.getMaterial());
        res.setLimited(p.getLimited());
        res.setDescription(p.getDescription());

        if (p.getCategories() != null) {
            res.setCategoryIds(p.getCategories().stream().map(Category::getId).collect(Collectors.toList()));
            res.setCategoryNames(p.getCategories().stream().map(Category::getName).collect(Collectors.toList()));
        }

        if (p.getImages() != null) {
            res.setImages(p.getImages().stream().map(img -> {
                ProductImageResponse imgRes = new ProductImageResponse();
                imgRes.setId(img.getId());
                imgRes.setUrl(img.getImageUrl());
                imgRes.setThumbnail(img.isThumbnail());
                return imgRes;
            }).collect(Collectors.toList()));
        }

        if (p.getVariants() != null) {
            res.setVariants(p.getVariants().stream().map(v -> {
                ProductVariantResponse vRes = new ProductVariantResponse();
                vRes.setId(v.getId());
                vRes.setSku(v.getSku());
                vRes.setSize(v.getSize());
                vRes.setSizeType(v.getSizeType());
                vRes.setColorway(v.getColorway());
                vRes.setStock(v.getStock());

                // 🔥 Lấy giá từ bảng ProductPrice
                BigDecimal activePrice = productPriceRepository.findByVariant_IdAndEndDateIsNull(v.getId())
                        .map(ProductPrice::getPrice)
                        .orElse(v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO);
                vRes.setPrice(activePrice);

                return vRes;
            }).collect(Collectors.toList()));
        }

        return res;
    }
}