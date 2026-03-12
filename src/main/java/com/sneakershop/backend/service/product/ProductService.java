package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.*;
import com.sneakershop.backend.entity.product.*;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.product.*;
import com.sneakershop.backend.repository.promotion.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductPriceRepository productPriceRepository;
    private final PromotionRepository promotionRepository;
    private final ProductTagRepository productTagRepository;
    private final ProductHistoryRepository productHistoryRepository;

    private static final Map<String, String> STATUS_LABEL = Map.of(
            "Còn hàng", "Còn hàng",
            "Hết hàng", "Hết hàng",
            "Đặt trước", "Đặt trước",
            "Ngừng bán", "Ngừng bán"
    );

    /* ================== QUẢN LÝ TÌM KIẾM ================== */

    public Page<ProductResponse> getProducts(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id").descending());
        }
        return productRepository.findAll(pageable).map(this::toListResponse);
    }

    public Page<ProductResponse> searchProducts(List<Long> categoryIds, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        long categoryCount = (categoryIds != null) ? categoryIds.size() : 0;
        return productRepository.searchProducts(categoryIds, keyword, categoryCount, pageable).map(this::toListResponse);
    }

    public Page<ProductResponse> searchAdvanced(ProductSearchRequest request, Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id").descending());
        }
        return productRepository.findAll(ProductSpecification.build(request), pageable).map(this::toListResponse);
    }

    public Page<ProductResponse> getBestSellingProducts(Pageable pageable) {
        return productRepository.findBestSellingProducts(pageable).map(this::toListResponse);
    }

    /* ================== CRUD OPERATIONS ================== */

    @Transactional
    @AuditAction(module = "PRODUCT", action = "CREATE", entity = "Product",
            description = "Thêm SP mới | Tên: #{#request.name} | SKU: #{#request.sku}")
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setSku(request.getSku());

        product.setBrand(request.getBrand());
        product.setModel(request.getModel());
        product.setReleaseYear(request.getReleaseYear());
        product.setGender(request.getGender());
        product.setReleaseType(request.getReleaseType());
        product.setMaterial(request.getMaterial());
        product.setLimited(request.getLimited());
        product.setDescription(request.getDescription());

        product.setStatus(request.getStatus() != null ? request.getStatus() : "Còn hàng");
        product.setThumbnail(request.getThumbnail());
        product.setCreatedAt(LocalDateTime.now());

        if (request.getCategoryIds() != null) {
            product.setCategories(categoryRepository.findAllById(request.getCategoryIds()));
        }

        if (request.getImages() != null) {
            List<ProductImage> images = request.getImages().stream().map(imgReq -> {
                ProductImage img = new ProductImage();
                img.setImageUrl(imgReq.getImageUrl());
                img.setThumbnail(imgReq.isThumbnail());
                img.setProduct(product);
                return img;
            }).collect(Collectors.toList());
            product.setImages(images);
        }

        if (request.getVariants() != null) {
            List<ProductVariant> variants = request.getVariants().stream().map(vReq -> {
                ProductVariant v = new ProductVariant();
                v.setProduct(product);
                v.setSize(vReq.getSize());
                v.setSizeType(vReq.getSizeType());
                v.setColorway(vReq.getColorway());

                // 🔥 LƯU ẢNH LÚC TẠO MỚI BIẾN THỂ
                v.setImageUrl(vReq.getImageUrl());

                v.setPrice(vReq.getPrice() != null ? vReq.getPrice() : BigDecimal.ZERO);
                v.setStock(vReq.getStock());
                v.setStatus(vReq.getStock() > 0 ? "Còn hàng" : "Hết hàng");
                v.setSku(generateVariantSku(product, vReq));
                return v;
            }).collect(Collectors.toList());
            product.setVariants(variants);
        }

        updateProductStatus(product);
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

        saveHistory(id, "name", p.getName(), request.getName());
        saveHistory(id, "brand", p.getBrand(), request.getBrand());
        saveHistory(id, "status", p.getStatus(), request.getStatus());
        saveHistory(id, "model", p.getModel(), request.getModel());
        saveHistory(id, "releaseYear", p.getReleaseYear(), request.getReleaseYear());

        p.setName(request.getName());
        p.setBrand(request.getBrand());
        p.setModel(request.getModel());
        p.setReleaseYear(request.getReleaseYear());
        p.setGender(request.getGender());
        p.setReleaseType(request.getReleaseType());
        p.setMaterial(request.getMaterial());
        p.setLimited(request.getLimited());
        p.setDescription(request.getDescription());

        p.setThumbnail(request.getThumbnail());
        p.setStatus(request.getStatus());

        if (request.getCategoryIds() != null) {
            p.setCategories(categoryRepository.findAllById(request.getCategoryIds()));
        }

        if (request.getImages() != null) {
            p.getImages().clear();
            List<ProductImage> newImages = request.getImages().stream().map(imgReq -> {
                ProductImage img = new ProductImage();
                img.setImageUrl(imgReq.getImageUrl());
                img.setThumbnail(imgReq.isThumbnail());
                img.setProduct(p);
                return img;
            }).collect(Collectors.toList());
            p.getImages().addAll(newImages);
        }

        if (request.getVariants() != null) {
            List<ProductVariant> currentVariants = p.getVariants();
            List<ProductVariant> processedVariants = new ArrayList<>();

            for (VariantRequest vReq : request.getVariants()) {
                Optional<ProductVariant> existingVariantOpt = Optional.empty();

                if (vReq.getId() != null) {
                    existingVariantOpt = currentVariants.stream()
                            .filter(v -> vReq.getId().equals(v.getId()))
                            .findFirst();
                }

                if (existingVariantOpt.isEmpty()) {
                    existingVariantOpt = currentVariants.stream()
                            .filter(v -> v.getSize().equals(vReq.getSize())
                                    && v.getColorway().equals(vReq.getColorway())
                                    && v.getSizeType().equals(vReq.getSizeType()))
                            .findFirst();
                }

                if (existingVariantOpt.isPresent()) {
                    ProductVariant existing = existingVariantOpt.get();
                    existing.setSize(vReq.getSize());
                    existing.setSizeType(vReq.getSizeType());
                    existing.setColorway(vReq.getColorway());

                    // 🔥 LƯU ẢNH VÀO BIẾN THỂ CŨ ĐANG UPDATE
                    existing.setImageUrl(vReq.getImageUrl());

                    existing.setStock(vReq.getStock());
                    existing.setStatus(vReq.getStock() > 0 ? "Còn hàng" : "Hết hàng");
                    existing.setPrice(vReq.getPrice() != null ? vReq.getPrice() : BigDecimal.ZERO);
                    existing.setSku(generateVariantSku(p, vReq));
                    processedVariants.add(existing);
                } else {
                    ProductVariant newVariant = new ProductVariant();
                    newVariant.setProduct(p);
                    newVariant.setSize(vReq.getSize());
                    newVariant.setSizeType(vReq.getSizeType());
                    newVariant.setColorway(vReq.getColorway());

                    // 🔥 LƯU ẢNH VÀO BIẾN THỂ MỚI TINH VỪA ĐƯỢC BẤM NÚT "THÊM"
                    newVariant.setImageUrl(vReq.getImageUrl());

                    newVariant.setPrice(vReq.getPrice() != null ? vReq.getPrice() : BigDecimal.ZERO);
                    newVariant.setStock(vReq.getStock());
                    newVariant.setStatus(vReq.getStock() > 0 ? "Còn hàng" : "Hết hàng");
                    newVariant.setSku(generateVariantSku(p, vReq));
                    processedVariants.add(newVariant);
                }
            }

            currentVariants.retainAll(processedVariants);
            for (ProductVariant variant : processedVariants) {
                if (!currentVariants.contains(variant)) {
                    currentVariants.add(variant);
                }
            }
        }

        updateProductStatus(p);
        return toListResponse(productRepository.save(p));
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "DELETE", entity = "Product",
            description = "Xóa SP ID: #{#id}")
    public void delete(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        product.setDeleted(true);
        product.setStatus("Ngừng bán");

        if (product.getVariants() != null) {
            for (ProductVariant variant : product.getVariants()) {
                variant.setStatus("Ngừng bán");
                variant.setStock(0);
                if (variant.getProductPrices() != null) {
                    for (ProductPrice price : variant.getProductPrices()) {
                        if (price.getEndDate() == null) {
                            price.setEndDate(LocalDateTime.now());
                        }
                    }
                }
            }
        }
        productRepository.save(product);
    }

    /* ================== MAPPING DỮ LIỆU ================== */

    private ProductResponse toListResponse(Product p) {
        ProductResponse res = new ProductResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setSku(p.getSku());
        res.setBrand(p.getBrand());
        res.setStatus(STATUS_LABEL.getOrDefault(p.getStatus(), p.getStatus()));
        res.setThumbnail(p.getThumbnail());

        if (p.getCategories() != null) {
            res.setCategoryNames(p.getCategories().stream().map(Category::getName).collect(Collectors.toList()));
        }

        boolean isNew = p.getCreatedAt() != null && p.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));
        res.setIsNew(isNew);

        try {
            Long sold = productRepository.countSoldByProduct(p.getId());
            res.setIsHot(sold != null && sold >= 50);
        } catch (Exception e) {
            res.setIsHot(false);
        }

        boolean hasPromotion = productRepository.hasActivePromotion(p.getId(), LocalDateTime.now());
        if (hasPromotion) {
            res.setDiscountedPrice(BigDecimal.ONE);
        }

        if (p.getTags() != null) {
            res.setTags(p.getTags().stream().map(ProductTag::getName).collect(Collectors.toList()));
        }
        return res;
    }

    private ProductDetailResponse toDetailResponse(Product p) {
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

                // 🔥 NHẢ ẢNH TỪ DATABASE VỀ FRONTEND
                vRes.setImageUrl(v.getImageUrl());

                vRes.setStock(v.getStock());

                BigDecimal activePrice = productPriceRepository.findByVariant_IdAndEndDateIsNull(v.getId())
                        .map(ProductPrice::getPrice)
                        .orElse(v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO);
                vRes.setPrice(activePrice);

                return vRes;
            }).collect(Collectors.toList()));
        }
        return res;
    }

    /* ================== CÁC HÀM BỔ TRỢ ================== */

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

    private String generateVariantSku(Product product, VariantRequest v) {
        return (product.getSku() + "-" + v.getColorway() + "-" + v.getSize()).toUpperCase().replace(" ", "");
    }

    private void updateProductStatus(Product product) {
        boolean hasStock = product.getVariants().stream().anyMatch(v -> v.getStock() > 0);
        product.setStatus(hasStock ? "Còn hàng" : "Hết hàng");
    }

    public Page<Product> getProductsByPromotion(Long promotionId, Pageable pageable) {
        return productRepository.findProductsByPromotion(promotionId, pageable);
    }

    @Transactional
    public void addTagToProduct(Long productId, Long tagId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        ProductTag tag = productTagRepository.findById(tagId).orElseThrow(() -> new RuntimeException("Tag not found"));
        if (product.getTags() == null) {
            product.setTags(new ArrayList<>());
        }
        product.getTags().add(tag);
        productRepository.save(product);
    }

    @Transactional
    public void updateProductTags(Long productId, List<Long> tagIds) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        List<ProductTag> tags = productTagRepository.findAllById(tagIds);
        product.setTags(tags);
        productRepository.save(product);
    }

    @Transactional
    public void removeTagFromProduct(Long productId, Long tagId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        product.getTags().removeIf(tag -> tag.getId().equals(tagId));
        productRepository.save(product);
    }

    public Page<ProductResponse> getProductsByTag(String tagName, Pageable pageable) {
        Page<Product> products = productRepository.findProductsInStock(pageable);
        List<ProductResponse> filtered = products.stream().map(this::toListResponse).filter(p -> {
            if ("NEW".equalsIgnoreCase(tagName)) return Boolean.TRUE.equals(p.getIsNew());
            if ("HOT".equalsIgnoreCase(tagName)) return Boolean.TRUE.equals(p.getIsHot());
            if ("SALE".equalsIgnoreCase(tagName)) return p.getDiscountedPrice() != null;
            return false;
        }).toList();
        return new PageImpl<>(filtered, pageable, filtered.size());
    }

    public Page<ProductResponse> getProductsByCreatedDate(LocalDate date, Pageable pageable) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        return productRepository.findProductsByCreatedDate(start, end, pageable).map(this::toListResponse);
    }

    public Page<ProductResponse> filterProductsByDate(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        return productRepository.findProductsByDateRange(start, end, pageable).map(this::toListResponse);
    }

    @Transactional
    public ProductResponse updateStatus(Long productId, String status) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStatus(status);
        productRepository.save(product);
        return toListResponse(product);
    }

    @Transactional
    public void batchUpdateStatus(List<Long> ids, String status) {
        List<Product> products = productRepository.findAllById(ids);
        for (Product product : products) {
            product.setStatus(status);
        }
        productRepository.saveAll(products);
    }

    public Page<Product> getUpdatedProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findAllByOrderByUpdatedAtDesc(pageable);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        List<Product> products = productRepository.findAllById(ids);
        for (Product product : products) {
            product.setDeleted(true);
            product.setStatus("Ngừng bán");
            if (product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {
                    variant.setStatus("Ngừng bán");
                    variant.setStock(0);
                    if (variant.getProductPrices() != null) {
                        for (ProductPrice price : variant.getProductPrices()) {
                            if (price.getEndDate() == null) {
                                price.setEndDate(LocalDateTime.now());
                            }
                        }
                    }
                }
            }
        }
        productRepository.saveAll(products);
    }

    private void saveHistory(Long productId, String field, Object oldValue, Object newValue) {
        if (oldValue == null && newValue == null) return;
        if (oldValue != null && oldValue.equals(newValue)) return;
        ProductHistory history = new ProductHistory();
        history.setProductId(productId);
        history.setFieldName(field);
        history.setOldValue(oldValue != null ? oldValue.toString() : null);
        history.setNewValue(newValue != null ? newValue.toString() : null);
        history.setUpdatedAt(LocalDateTime.now());
        productHistoryRepository.save(history);
    }

    public List<ProductHistoryResponse> getProductHistory(Long productId) {
        return productHistoryRepository.findByProductIdOrderByUpdatedAtDesc(productId).stream().map(h -> {
            ProductHistoryResponse res = new ProductHistoryResponse();
            res.setFieldName(h.getFieldName());
            res.setOldValue(h.getOldValue());
            res.setNewValue(h.getNewValue());
            res.setUpdatedAt(h.getUpdatedAt());
            return res;
        }).toList();
    }
}