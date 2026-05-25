package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.ProductDetailResponse;
import com.sneakershop.backend.dto.product.ProductImageResponse;
import com.sneakershop.backend.dto.product.ProductVariantResponse;
import com.sneakershop.backend.dto.product.StorefrontHomeProductResponse;
import com.sneakershop.backend.entity.product.Category;
import com.sneakershop.backend.dto.product.StorefrontHomeVariantResponse;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.product.ProductImage;
import com.sneakershop.backend.entity.product.ProductTag;
import org.springframework.transaction.annotation.Transactional;
import com.sneakershop.backend.entity.promotion.DiscountType;
import com.sneakershop.backend.entity.promotion.PromotionDetail;
import com.sneakershop.backend.repository.product.ProductRepository;
import com.sneakershop.backend.repository.product.ProductImageRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import com.sneakershop.backend.repository.promotion.PromotionDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StorefrontProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final PromotionDetailRepository promotionDetailRepository;

    @Transactional(readOnly = true)
    public List<StorefrontHomeProductResponse> getHomeProducts() {
        return productRepository.findAllForStorefrontHome().stream()
                .map(this::toHomeResponse)
                .filter(Objects::nonNull)
                .toList();
    }
    private int getAvailableStock(ProductVariant variant) {
        if (variant == null) return 0;
        return Math.max(variant.getStock() - Math.max(variant.getReserved_quantity(), 0), 0);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long id) {
        Product p = productRepository.findDetailById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        ProductDetailResponse res = new ProductDetailResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setSku(p.getSku());
        res.setStatus(p.getStatus());
        res.setThumbnail(p.getThumbnail());
        res.setBrand(p.getBrand());
        res.setModel(p.getModel());
        res.setReleaseYear(p.getReleaseYear());
        res.setGender(p.getGender());
        res.setReleaseType(p.getReleaseType());
        res.setMaterial(p.getMaterial() != null ? p.getMaterial().getName() : null);
        res.setSole(p.getSole() != null ? p.getSole().getName() : null);
        res.setLimited(p.getLimited());
        res.setDescription(p.getDescription());

        if (p.getCategories() != null) {
            res.setCategoryIds(p.getCategories().stream().map(Category::getId).toList());
            res.setCategoryNames(p.getCategories().stream().map(Category::getName).toList());
        }

        List<ProductImage> productImages = productImageRepository.findByProductId(p.getId());
        if (productImages != null) {
            res.setImages(productImages.stream().map(img -> {
                ProductImageResponse x = new ProductImageResponse();
                x.setId(img.getId());
                x.setUrl(img.getImageUrl());
                x.setThumbnail(img.isThumbnail());
                return x;
            }).toList());
        }

        List<ProductVariant> variants = productVariantRepository.findByProduct_Id(p.getId());
        if (variants == null) {
            variants = List.of();
        }

        List<ProductVariantResponse> variantResponses = variants.stream()
                .filter(v -> !"Ngừng bán".equalsIgnoreCase(v.getStatus()))
                .map(v -> {
                    ProductVariantResponse x = new ProductVariantResponse();
                    x.setId(v.getId());
                    x.setSku(v.getSku());
                    x.setSize(v.getSize() != null ? v.getSize().getName() : null);
                    x.setColorway(v.getColor() != null ? v.getColor().getName() : null);
                    x.setImageUrl(v.getImageUrl());
                    x.setStock(getAvailableStock(v));
                    x.setReservedQuantity(Math.max(v.getReserved_quantity(), 0));
                    x.setAvailableStock(getAvailableStock(v));
                    x.setStatus(v.getStatus());

                    // Yêu cầu sửa: Cập nhật logic giá cho variant
                    BigDecimal originalPrice = v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO;
                    BigDecimal salePrice = resolveBestSalePrice(v, originalPrice);
                    BigDecimal displayPrice = salePrice != null ? salePrice : originalPrice;

                    x.setOriginalPrice(originalPrice);
                    x.setSalePrice(salePrice);
                    x.setPrice(displayPrice);

                    return x;
                }).toList();

        res.setVariants(variantResponses);
        res.setThumbnail(resolveProductThumbnail(p, productImages, variants));
        return res;
    }

    private String resolveProductThumbnail(Product product, List<ProductImage> images, List<ProductVariant> variants) {
        if (product != null && !isBlank(product.getThumbnail())) {
            return product.getThumbnail();
        }

        if (images != null) {
            ProductImage thumbnail = images.stream()
                    .filter(Objects::nonNull)
                    .filter(ProductImage::isThumbnail)
                    .filter(img -> !isBlank(img.getImageUrl()))
                    .findFirst()
                    .orElse(null);

            if (thumbnail != null) {
                return thumbnail.getImageUrl();
            }

            ProductImage firstImage = images.stream()
                    .filter(Objects::nonNull)
                    .filter(img -> !isBlank(img.getImageUrl()))
                    .findFirst()
                    .orElse(null);

            if (firstImage != null) {
                return firstImage.getImageUrl();
            }
        }

        if (variants != null) {
            return variants.stream()
                    .filter(Objects::nonNull)
                    .map(ProductVariant::getImageUrl)
                    .filter(url -> !isBlank(url))
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private StorefrontHomeProductResponse toHomeResponse(Product product) {
        if (product == null) return null;

        List<ProductVariant> validVariants = product.getVariants() == null
                ? List.of()
                : product.getVariants().stream()
                  .filter(Objects::nonNull)
                  .filter(v -> !v.isDeleted())
                  .filter(v -> getAvailableStock(v) > 0)
                  .filter(v -> !"Ngừng bán".equalsIgnoreCase(v.getStatus()))
                  .filter(v -> v.getPrice() != null && v.getPrice().compareTo(BigDecimal.ZERO) > 0)
                  .toList();

        if (validVariants.isEmpty()) {
            return null;
        }

        ProductVariant displayVariant = validVariants.stream()
                .min(Comparator.comparing(ProductVariant::getPrice).thenComparing(ProductVariant::getId))
                .orElse(null);

        if (displayVariant == null) {
            return null;
        }

        List<StorefrontHomeVariantResponse> variantResponses = validVariants.stream()
                .map(v -> {
                    StorefrontHomeVariantResponse x = new StorefrontHomeVariantResponse();

                    BigDecimal originalPrice = v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO;
                    BigDecimal salePrice = resolveBestSalePrice(v, originalPrice);
                    BigDecimal displayPrice = salePrice != null ? salePrice : originalPrice;

                    x.setId(v.getId());
                    x.setSku(v.getSku());
                    x.setColorway(v.getColor() != null ? v.getColor().getName() : null);
                    x.setSize(v.getSize() != null ? v.getSize().getName() : null);
                    x.setStock(getAvailableStock(v));
                    x.setReservedQuantity(Math.max(v.getReserved_quantity(), 0));
                    x.setAvailableStock(getAvailableStock(v));
                    x.setImageUrl(v.getImageUrl());
                    x.setOriginalPrice(originalPrice);
                    x.setSalePrice(salePrice);
                    x.setPrice(displayPrice);
                    x.setStatus(v.getStatus());

                    return x;
                })
                .toList();

        BigDecimal originalPrice = displayVariant.getPrice();
        BigDecimal salePrice = resolveBestSalePrice(displayVariant, originalPrice);

        List<ProductImage> productImages = productImageRepository.findByProductId(product.getId());

        StorefrontHomeProductResponse res = new StorefrontHomeProductResponse();
        res.setId(product.getId());
        res.setProductName(product.getName());
        res.setThumbnail(resolveProductThumbnail(product, productImages, validVariants));
        res.setBrand(product.getBrand());
        res.setGender(product.getGender());
        res.setStatus(product.getStatus());
        res.setCreatedAt(product.getCreatedAt() != null ? product.getCreatedAt().toString() : null);

        if (product.getCategories() != null) {
            List<Long> categoryIds = product.getCategories().stream().map(Category::getId).toList();
            List<String> categoryNames = product.getCategories().stream().map(Category::getName).toList();

            res.setCategoryIds(categoryIds);
            res.setCategoryNames(categoryNames);
            res.setCategoryName(categoryNames.isEmpty() ? "" : categoryNames.get(0));
        }

        res.setDisplayVariantId(displayVariant.getId());
        res.setDisplayVariantSku(displayVariant.getSku());
        res.setDisplayColorway(displayVariant.getColor() != null ? displayVariant.getColor().getName() : null);
        res.setDisplaySize(displayVariant.getSize() != null ? displayVariant.getSize().getName() : null);
        res.setDefaultVariantId(displayVariant.getId());

        res.setOriginalPrice(originalPrice);
        res.setSalePrice(salePrice);
        res.setOnSale(salePrice != null && salePrice.compareTo(originalPrice) < 0);

        boolean isNew = product.getCreatedAt() != null
                && product.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));
        res.setIsNew(isNew);

        boolean hasHotTag = product.getTags() != null && product.getTags().stream()
                .map(ProductTag::getName)
                .anyMatch(tag -> "HOT".equalsIgnoreCase(tag));
        res.setIsHot(hasHotTag);

        String badge = null;
        if (Boolean.TRUE.equals(res.getIsNew())) badge = "NEW";
        else if (Boolean.TRUE.equals(res.getOnSale())) badge = "SALE";
        else if (Boolean.TRUE.equals(res.getIsHot())) badge = "HOT";
        res.setBadge(badge);

        res.setDetailUrl("/trang-chu/san-pham/" + product.getId());
        res.setVariants(variantResponses);

        return res;
    }

    private BigDecimal resolveBestSalePrice(ProductVariant variant, BigDecimal originalPrice) {
        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        List<PromotionDetail> activeDetails = promotionDetailRepository.findAllActiveByVariantId(
                variant.getId(),
                LocalDateTime.now()
        );

        if (activeDetails == null || activeDetails.isEmpty()) {
            return null;
        }

        BigDecimal bestFinal = activeDetails.stream()
                .map(detail -> calculateDiscountedPrice(originalPrice, detail))
                .min(BigDecimal::compareTo)
                .orElse(originalPrice);

        return bestFinal.compareTo(originalPrice) < 0 ? bestFinal : null;
    }

    private BigDecimal calculateDiscountedPrice(BigDecimal originalPrice, PromotionDetail detail) {
        if (detail == null || detail.getDiscountType() == null || detail.getDiscountValue() == null) {
            return originalPrice;
        }

        BigDecimal finalPrice = originalPrice;

        if (detail.getDiscountType() == DiscountType.PERCENT) {
            BigDecimal discount = originalPrice
                    .multiply(detail.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            finalPrice = originalPrice.subtract(discount);
        } else if (detail.getDiscountType() == DiscountType.AMOUNT) {
            finalPrice = originalPrice.subtract(detail.getDiscountValue());
        }

        return finalPrice.max(BigDecimal.ZERO);
    }
}