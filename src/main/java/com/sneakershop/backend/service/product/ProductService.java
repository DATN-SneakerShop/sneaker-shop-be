package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.*;
import com.sneakershop.backend.entity.product.*;
import com.sneakershop.backend.repository.product.*;
import com.sneakershop.backend.exception.ValidationException;
import com.sneakershop.backend.service.ValidationSupport;
import com.sneakershop.backend.repository.promotion.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final PromotionRepository promotionRepository;
    private final ProductTagRepository productTagRepository;
    private final ProductHistoryRepository productHistoryRepository;
    private final EntityManager entityManager;

    private static final Map<String, String> STATUS_LABEL = Map.of(
            "Còn hàng", "Còn hàng",
            "Hết hàng", "Hết hàng",
            "Đặt trước", "Đặt trước",
            "Ngừng bán", "Ngừng bán"
    );

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

    @Transactional
    @AuditAction(module = "PRODUCT", action = "CREATE", entity = "Product", description = "Thêm SP mới")
    public ProductResponse create(ProductRequest request) {
        validateProductRequest(request, null);
        Product product = new Product();
        product.setName(ValidationSupport.trim(request.getName()));
        product.setSku(ValidationSupport.trim(request.getSku()));

        product.setBrand(request.getBrand());
        product.setModel(request.getModel());
        product.setReleaseYear(request.getReleaseYear());
        product.setGender(request.getGender());
        product.setReleaseType(request.getReleaseType());
        product.setLimited(request.getLimited());
        product.setMaterial(mapMaterial(request.getMaterial()));
        product.setSole(mapSole(request.getSole()));
        product.setDescription(request.getDescription());

        // Set trạng thái ban đầu do admin yêu cầu
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
                Size resolvedSize = mapSize(vReq.getSize());
                Color resolvedColor = mapColor(vReq.getColorway());

                ProductVariant v = new ProductVariant();
                v.setProduct(product);
                v.setSize(resolvedSize);
                v.setColor(resolvedColor);
                v.setImageUrl(vReq.getImageUrl());
                v.setPrice(vReq.getPrice() != null ? vReq.getPrice() : BigDecimal.ZERO);
                v.setStock(vReq.getStock());
                v.setStatus(vReq.getStock() > 0 ? "Còn hàng" : "Hết hàng");

                if (vReq.getSku() != null && !vReq.getSku().trim().isEmpty()) {
                    v.setSku(ValidationSupport.trim(vReq.getSku()));
                } else {
                    v.setSku(generateVariantSku(product, vReq));
                }

                return v;
            }).collect(Collectors.toList());
            product.setVariants(variants);
        }

        updateProductStatus(product); // Cập nhật lại kho nếu cần
        Product savedProduct = productRepository.save(product);

        return toListResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getById(Long id) {
        Product p = productRepository.findDetailById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        return toDetailResponse(p);
    }

    @Transactional
    @AuditAction(module = "PRODUCT", action = "UPDATE", entity = "Product", description = "Sửa SP ID #{#id}")
    public ProductResponse update(Long id, ProductRequest request) {
        Product p = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));

        saveHistory(id, "name", p.getName(), request.getName());
        saveHistory(id, "brand", p.getBrand(), request.getBrand());
        saveHistory(id, "status", p.getStatus(), request.getStatus());
        saveHistory(id, "model", p.getModel(), request.getModel());
        saveHistory(id, "releaseYear", p.getReleaseYear(), request.getReleaseYear());
        saveHistory(id, "material", p.getMaterial() != null ? p.getMaterial().getName() : null, request.getMaterial());
        saveHistory(id, "sole", p.getSole() != null ? p.getSole().getName() : null, request.getSole());

        validateProductRequest(request, id);

        p.setName(ValidationSupport.trim(request.getName()));
        p.setSku(ValidationSupport.trim(request.getSku()));
        p.setBrand(request.getBrand());
        p.setModel(request.getModel());
        p.setReleaseYear(request.getReleaseYear());
        p.setGender(request.getGender());
        p.setReleaseType(request.getReleaseType());
        p.setLimited(request.getLimited());
        p.setMaterial(mapMaterial(request.getMaterial()));
        p.setSole(mapSole(request.getSole()));
        p.setDescription(request.getDescription());
        p.setThumbnail(request.getThumbnail());
        p.setStatus(request.getStatus());

        // FIX: Cập nhật Collection ManyToMany chuẩn xác của Hibernate
        if (request.getCategoryIds() != null) {
            if (p.getCategories() == null) {
                p.setCategories(new ArrayList<>());
            }
            p.getCategories().clear();
            p.getCategories().addAll(categoryRepository.findAllById(request.getCategoryIds()));
        }

        if (request.getImages() != null) {
            p.getImages().clear();
            p.getImages().addAll(request.getImages().stream().map(imgReq -> {
                ProductImage img = new ProductImage();
                img.setImageUrl(imgReq.getImageUrl());
                img.setThumbnail(imgReq.isThumbnail());
                img.setProduct(p);
                return img;
            }).collect(Collectors.toList()));
        }

        if (request.getVariants() != null) {
            List<ProductVariant> currentVariants = p.getVariants();

            List<Long> incomingIds = request.getVariants().stream()
                    .map(VariantRequest::getId).filter(Objects::nonNull).collect(Collectors.toList());

            List<ProductVariant> toDelete = currentVariants.stream()
                    .filter(v -> v.getId() != null && !incomingIds.contains(v.getId())).collect(Collectors.toList());

            for (ProductVariant v : toDelete) {
                if (v.getVariantPriceGroups() != null) v.getVariantPriceGroups().clear();
            }

            currentVariants.removeAll(toDelete);
            entityManager.flush();

            for (VariantRequest vReq : request.getVariants()) {
                Size resolvedSize = mapSize(vReq.getSize());
                Color resolvedColor = mapColor(vReq.getColorway());

                Optional<ProductVariant> existingVariantOpt = Optional.empty();
                if (vReq.getId() != null) {
                    existingVariantOpt = currentVariants.stream().filter(v -> vReq.getId().equals(v.getId())).findFirst();
                }

                if (existingVariantOpt.isEmpty()) {
                    existingVariantOpt = currentVariants.stream()
                            .filter(v -> matchSize(v.getSize(), vReq.getSize())
                                    && matchColor(v.getColor(), vReq.getColorway()))
                            .findFirst();
                }

                ProductVariant variant;
                boolean isNew = false;

                if (existingVariantOpt.isPresent()) {
                    variant = existingVariantOpt.get();
                } else {
                    variant = new ProductVariant();
                    variant.setProduct(p);
                    isNew = true;
                }

                variant.setSize(resolvedSize);
                variant.setColor(resolvedColor);
                variant.setImageUrl(vReq.getImageUrl());
                validateStockNotBelowReserved(variant, vReq.getStock());
                variant.setStock(vReq.getStock());
                variant.setStatus(vReq.getStock() > 0 ? "Còn hàng" : "Hết hàng");
                variant.setPrice(vReq.getPrice() != null ? vReq.getPrice() : BigDecimal.ZERO);

                if (vReq.getSku() != null && !vReq.getSku().trim().isEmpty()) {
                    variant.setSku(vReq.getSku().trim());
                } else {
                    variant.setSku(generateVariantSku(p, vReq));
                }

                if (isNew) {
                    currentVariants.add(variant);
                }
            }
        }

        updateProductStatus(p);
        Product savedProduct = productRepository.save(p);

        return toListResponse(savedProduct);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        product.setDeleted(true);
        product.setStatus("Ngừng bán");
        if (product.getVariants() != null) {
            for (ProductVariant variant : product.getVariants()) {
                validateStockNotBelowReserved(variant, 0);
                variant.setStatus("Ngừng bán");
                variant.setStock(0);
            }
        }
        productRepository.save(product);
    }

    private void validateStockNotBelowReserved(ProductVariant variant, int newStock) {
        if (variant == null) return;
        int reserved = Math.max(variant.getReserved_quantity(), 0);
        if (newStock < reserved) {
            throw new ValidationException("stock", "Tồn kho mới không được nhỏ hơn số lượng đang giữ chỗ: " + reserved + ". Vui lòng xử lý/hủy đơn liên quan trước.");
        }
    }

    private ProductResponse toListResponse(Product p) {
        ProductResponse res = new ProductResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setSku(p.getSku());
        res.setBrand(p.getBrand());
        res.setStatus(STATUS_LABEL.getOrDefault(p.getStatus(), p.getStatus()));
        res.setThumbnail(p.getThumbnail());
        res.setMaterial(p.getMaterial() != null ? p.getMaterial().getName() : null);
        res.setSole(p.getSole() != null ? p.getSole().getName() : null);

        // Bổ sung map ID cho Frontend xử lý filter Many-to-Many
        if (p.getCategories() != null) {
            res.setCategoryNames(p.getCategories().stream().map(Category::getName).collect(Collectors.toList()));
            // Giả định ProductResponse có setter categoryIds, nếu báo lỗi bạn thêm field `List<Long> categoryIds` vào DTO nhé.
            try {
                res.setCategoryIds(p.getCategories().stream().map(Category::getId).collect(Collectors.toList()));
            } catch (Exception ignored) {
                // Ignore nếu DTO chưa có hàm setCategoryIds
            }
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
        if (hasPromotion) res.setDiscountedPrice(BigDecimal.ONE); // Cờ báo hiệu có giảm giá

        if (p.getTags() != null) res.setTags(p.getTags().stream().map(ProductTag::getName).collect(Collectors.toList()));
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
        res.setMaterial(p.getMaterial() != null ? p.getMaterial().getName() : null);
        res.setSole(p.getSole() != null ? p.getSole().getName() : null);
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
                vRes.setSize(v.getSize() != null ? v.getSize().getName() : null);
                vRes.setColorway(v.getColor() != null ? v.getColor().getName() : null);
                vRes.setImageUrl(v.getImageUrl());
                vRes.setStock(v.getStock());
                vRes.setReservedQuantity(Math.max(v.getReserved_quantity(), 0));
                vRes.setAvailableStock(Math.max(0, v.getStock() - Math.max(v.getReserved_quantity(), 0)));
                vRes.setPrice(v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO);
                return vRes;
            }).collect(Collectors.toList()));
        }
        return res;
    }

    public List<ProductSimpleResponse> getAll() {
        return productRepository.findAllSimpleWithVariantCount();
    }

    public List<ProductSimpleResponse> getAllForPromotionEdit(Long promotionId) {
        return productRepository.findAllSimpleForPromotionEdit(promotionId);
    }

    public List<VariantResponse> getVariantsByProduct(Long productId) {
        return productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found"))
                .getVariants().stream().map(v -> {
                    VariantResponse vr = new VariantResponse();
                    vr.setVariantId(v.getId());
                    vr.setSku(v.getSku());
                    vr.setSize(v.getSize() != null ? v.getSize().getName() : null);
                    vr.setColorway(v.getColor() != null ? v.getColor().getName() : null);
                    vr.setStock(v.getStock());
                    vr.setReservedQuantity(Math.max(v.getReserved_quantity(), 0));
                    vr.setAvailableStock(Math.max(0, v.getStock() - Math.max(v.getReserved_quantity(), 0)));
                    vr.setPrice(v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO);
                    return vr;
                }).collect(Collectors.toList());
    }


    private void validateProductRequest(ProductRequest request, Long currentProductId) {
        String name = ValidationSupport.trim(request.getName());
        String sku = ValidationSupport.trim(request.getSku());
        if (name == null) throw new ValidationException("name", "Tên sản phẩm không được để trống.");
        if (sku == null) throw new ValidationException("sku", "Mã sản phẩm không được để trống.");
        request.setName(name);
        request.setSku(sku);

        boolean duplicateName = currentProductId == null
                ? productRepository.existsByNameNormalized(name)
                : productRepository.existsByNameNormalizedAndIdNot(name, currentProductId);
        if (duplicateName) throw new ValidationException("name", "Tên sản phẩm đã tồn tại.");

        boolean duplicateSku = currentProductId == null
                ? productRepository.existsBySkuNormalized(sku)
                : productRepository.existsBySkuNormalizedAndIdNot(sku, currentProductId);
        if (duplicateSku) throw new ValidationException("sku", "Mã sản phẩm đã được sử dụng.");

        validateVariantPayload(request.getVariants(), currentProductId);
    }

    private void validateVariantPayload(List<VariantRequest> variants, Long productId) {
        if (variants == null || variants.isEmpty()) return;
        Set<String> skuSet = new HashSet<>();
        Set<String> comboSet = new HashSet<>();
        for (VariantRequest vReq : variants) {
            String sku = ValidationSupport.trim(vReq.getSku());
            if (sku != null) {
                vReq.setSku(sku);
                String skuKey = sku.toLowerCase();
                if (!skuSet.add(skuKey)) throw new ValidationException("sku", "SKU đã được sử dụng.");
                boolean duplicateSku = vReq.getId() == null
                        ? productVariantRepository.existsBySkuNormalized(sku)
                        : productVariantRepository.existsBySkuNormalizedAndIdNot(sku, vReq.getId());
                if (duplicateSku) throw new ValidationException("sku", "Mã SKU biến thể đã được sử dụng.");
            }
            String size = ValidationSupport.trim(vReq.getSize());
            String color = ValidationSupport.trim(vReq.getColorway());
            vReq.setSize(size);
            vReq.setColorway(color);
            String comboKey = (size == null ? "" : size.toLowerCase()) + "|" + (color == null ? "" : color.toLowerCase());
            if (!comboSet.add(comboKey)) {
                throw new ValidationException("variants", "Biến thể Size " + (size == null ? "" : size) + " - Màu " + (color == null ? "" : color) + " đã tồn tại.");
            }
            if (productId != null && size != null && color != null) {
                Size resolvedSize = mapSize(size);
                Color resolvedColor = mapColor(color);
                if (resolvedSize != null && resolvedSize.getId() != null && resolvedColor != null && resolvedColor.getId() != null) {
                    boolean duplicateCombo = vReq.getId() == null
                            ? productVariantRepository.existsByProductSizeColor(productId, resolvedSize.getId(), resolvedColor.getId())
                            : productVariantRepository.existsByProductSizeColorAndIdNot(productId, resolvedSize.getId(), resolvedColor.getId(), vReq.getId());
                    if (duplicateCombo) {
                        throw new ValidationException("variants", "Biến thể Size " + size + " - Màu " + color + " đã tồn tại.");
                    }
                }
            }
            if (vReq.getStock() < 0) throw new ValidationException("stock", "Số lượng sản phẩm không hợp lệ.");
        }
    }

    private String generateVariantSku(Product product, VariantRequest v) {
        String baseSku = product.getSku() != null ? product.getSku() : "PRD";
        StringBuilder sku = new StringBuilder(baseSku + "-" + v.getColorway());
        sku.append("-").append(v.getSize());
        return sku.toString().toUpperCase().replace(" ", "");
    }

    private void updateProductStatus(Product product) {
        // Tôn trọng trạng thái đặc biệt do admin set (ví dụ hàng đặt trước thì dù không có stock vẫn hiển thị)
        if ("Ngừng bán".equals(product.getStatus()) || "Đặt trước".equals(product.getStatus())) {
            return;
        }

        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            product.setStatus("Hết hàng");
            return;
        }

        boolean hasStock = product.getVariants().stream().anyMatch(v -> v.getStock() > 0);
        product.setStatus(hasStock ? "Còn hàng" : "Hết hàng");
    }

    public Page<Product> getProductsByPromotion(Long promotionId, Pageable pageable) {
        return productRepository.findProductsByPromotion(promotionId, pageable);
    }

    @Transactional
    public void addTagToProduct(Long productId, Long tagId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found"));
        ProductTag tag = productTagRepository.findById(tagId).orElseThrow(() -> new RuntimeException("Not found"));
        if (product.getTags() == null) product.setTags(new ArrayList<>());
        product.getTags().add(tag);
        productRepository.save(product);
    }

    @Transactional
    public void updateProductTags(Long productId, List<Long> tagIds) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found"));
        List<ProductTag> tags = productTagRepository.findAllById(tagIds);
        product.setTags(tags);
        productRepository.save(product);
    }

    @Transactional
    public void removeTagFromProduct(Long productId, Long tagId) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found"));
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
        return productRepository.findProductsByCreatedDate(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), pageable).map(this::toListResponse);
    }

    public Page<ProductResponse> filterProductsByDate(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return productRepository.findProductsByDateRange(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), pageable).map(this::toListResponse);
    }

    @Transactional
    public ProductResponse updateStatus(Long productId, String status) {
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found"));
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
        return productRepository.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, size));
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        List<Product> products = productRepository.findAllById(ids);
        for (Product product : products) {
            product.setDeleted(true);
            product.setStatus("Ngừng bán");
            if (product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {
                    validateStockNotBelowReserved(variant, 0);
                    variant.setStatus("Ngừng bán");
                    variant.setStock(0);
                }
            }
        }
        productRepository.saveAll(products);
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

    private Size mapSize(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        String v = val.trim();
        List<Size> list = entityManager.createQuery("SELECT x FROM Size x WHERE x.name = :n", Size.class).setParameter("n", v).getResultList();
        if (!list.isEmpty()) return list.get(0);
        Size s = new Size(); s.setName(v); entityManager.persist(s); return s;
    }

    private Color mapColor(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        String v = val.trim();
        List<Color> list = entityManager.createQuery("SELECT x FROM Color x WHERE x.name = :n", Color.class).setParameter("n", v).getResultList();
        if (!list.isEmpty()) return list.get(0);
        Color c = new Color(); c.setName(v); entityManager.persist(c); return c;
    }

    private Material mapMaterial(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        String v = val.trim();
        List<Material> list = entityManager.createQuery("SELECT x FROM Material x WHERE x.name = :n", Material.class).setParameter("n", v).getResultList();
        if (!list.isEmpty()) return list.get(0);
        Material m = new Material(); m.setName(v); entityManager.persist(m); return m;
    }

    private Sole mapSole(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        String v = val.trim();
        List<Sole> list = entityManager.createQuery("SELECT x FROM Sole x WHERE x.name = :n", Sole.class).setParameter("n", v).getResultList();
        if (!list.isEmpty()) return list.get(0);
        Sole s = new Sole(); s.setName(v); entityManager.persist(s); return s;
    }

    private boolean matchSize(Size s, String req) {
        if (s == null && (req == null || req.trim().isEmpty())) return true;
        if (s == null || req == null || req.trim().isEmpty()) return false;
        return s.getName().equalsIgnoreCase(req.trim());
    }

    private boolean matchColor(Color c, String req) {
        if (c == null && (req == null || req.trim().isEmpty())) return true;
        if (c == null || req == null || req.trim().isEmpty()) return false;
        return c.getName().equalsIgnoreCase(req.trim());
    }
}