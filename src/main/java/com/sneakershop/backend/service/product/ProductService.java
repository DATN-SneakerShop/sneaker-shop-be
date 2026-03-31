package com.sneakershop.backend.service.product;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.product.*;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.product.*;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.product.*;
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
    private final ProductPriceRepository productPriceRepository;
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
        Product product = new Product();
        product.setName(request.getName());
        product.setSku(request.getSku());

        product.setBrand(request.getBrand());
        product.setModel(request.getModel());
        product.setReleaseYear(request.getReleaseYear());
        product.setGender(request.getGender());
        product.setReleaseType(request.getReleaseType());
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
                Size resolvedSize = mapSize(vReq.getSize());
                Color resolvedColor = mapColor(vReq.getColorway());
                Material resolvedMaterial = mapMaterial(vReq.getMaterial());
                Sole resolvedSole = mapSole(vReq.getSole());

                ProductVariant v = new ProductVariant();
                v.setProduct(product);
                v.setSize(resolvedSize);
                v.setColor(resolvedColor);
                v.setMaterial(resolvedMaterial);
                v.setSole(resolvedSole);
                v.setImageUrl(vReq.getImageUrl());
                v.setPrice(vReq.getPrice() != null ? vReq.getPrice() : BigDecimal.ZERO);
                v.setStock(vReq.getStock());
                v.setStatus(vReq.getStock() > 0 ? "Còn hàng" : "Hết hàng");

                if (vReq.getSku() != null && !vReq.getSku().trim().isEmpty()) {
                    v.setSku(vReq.getSku().trim());
                } else {
                    v.setSku(generateVariantSku(product, vReq));
                }

                return v;
            }).collect(Collectors.toList());
            product.setVariants(variants);
        }

        updateProductStatus(product);
        Product savedProduct = productRepository.save(product);

        if (request.getVariants() != null && savedProduct.getVariants() != null) {
            for (ProductVariant savedVariant : savedProduct.getVariants()) {
                BigDecimal reqPrice = request.getVariants().stream()
                        .filter(r -> matchSize(savedVariant.getSize(), r.getSize()) && matchColor(savedVariant.getColor(), r.getColorway()))
                        .map(VariantRequest::getPrice).findFirst().orElse(BigDecimal.ZERO);
                syncVariantPrice(savedVariant, reqPrice);
            }
        }

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

        p.setName(request.getName());
        p.setBrand(request.getBrand());
        p.setModel(request.getModel());
        p.setReleaseYear(request.getReleaseYear());
        p.setGender(request.getGender());
        p.setReleaseType(request.getReleaseType());
        p.setLimited(request.getLimited());
        p.setDescription(request.getDescription());
        p.setThumbnail(request.getThumbnail());
        p.setStatus(request.getStatus());

        if (request.getCategoryIds() != null) {
            p.setCategories(categoryRepository.findAllById(request.getCategoryIds()));
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
                if (v.getProductPrices() != null) v.getProductPrices().clear();
                if (v.getVariantPriceGroups() != null) v.getVariantPriceGroups().clear();
            }

            currentVariants.removeAll(toDelete);
            entityManager.flush();

            for (VariantRequest vReq : request.getVariants()) {
                Size resolvedSize = mapSize(vReq.getSize());
                Color resolvedColor = mapColor(vReq.getColorway());
                Material resolvedMaterial = mapMaterial(vReq.getMaterial());
                Sole resolvedSole = mapSole(vReq.getSole());

                Optional<ProductVariant> existingVariantOpt = Optional.empty();
                if (vReq.getId() != null) {
                    existingVariantOpt = currentVariants.stream().filter(v -> vReq.getId().equals(v.getId())).findFirst();
                }

                if (existingVariantOpt.isEmpty()) {
                    existingVariantOpt = currentVariants.stream()
                            .filter(v -> matchSize(v.getSize(), vReq.getSize())
                                    && matchColor(v.getColor(), vReq.getColorway())
                                    && matchMaterial(v.getMaterial(), vReq.getMaterial())
                                    && matchSole(v.getSole(), vReq.getSole()))
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
                variant.setMaterial(resolvedMaterial);
                variant.setSole(resolvedSole);
                variant.setImageUrl(vReq.getImageUrl());
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

        if (request.getVariants() != null && savedProduct.getVariants() != null) {
            for (ProductVariant savedVariant : savedProduct.getVariants()) {
                BigDecimal reqPrice = request.getVariants().stream()
                        .filter(r -> matchSize(savedVariant.getSize(), r.getSize()) && matchColor(savedVariant.getColor(), r.getColorway()))
                        .map(VariantRequest::getPrice).findFirst().orElse(BigDecimal.ZERO);
                syncVariantPrice(savedVariant, reqPrice);
            }
        }

        return toListResponse(savedProduct);
    }

    private void syncVariantPrice(ProductVariant variant, BigDecimal newPrice) {
        if (newPrice == null) return;
        Optional<ProductPrice> currentPriceOpt = productPriceRepository.findByVariant_IdAndEndDateIsNull(variant.getId());
        if (currentPriceOpt.isPresent()) {
            ProductPrice currentPrice = currentPriceOpt.get();
            if (currentPrice.getPrice().compareTo(newPrice) == 0) return;
            currentPrice.setEndDate(LocalDateTime.now());
            productPriceRepository.save(currentPrice);
        }
        ProductPrice newProductPrice = new ProductPrice();
        newProductPrice.setVariant(variant);
        newProductPrice.setPrice(newPrice);
        newProductPrice.setStartDate(LocalDateTime.now());
        productPriceRepository.save(newProductPrice);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
        product.setDeleted(true); product.setStatus("Ngừng bán");
        if (product.getVariants() != null) {
            for (ProductVariant variant : product.getVariants()) {
                variant.setStatus("Ngừng bán"); variant.setStock(0);
                if (variant.getProductPrices() != null) {
                    for (ProductPrice price : variant.getProductPrices()) {
                        if (price.getEndDate() == null) price.setEndDate(LocalDateTime.now());
                    }
                }
            }
        }
        productRepository.save(product);
    }

    private ProductResponse toListResponse(Product p) {
        ProductResponse res = new ProductResponse(); res.setId(p.getId()); res.setName(p.getName()); res.setSku(p.getSku()); res.setBrand(p.getBrand()); res.setStatus(STATUS_LABEL.getOrDefault(p.getStatus(), p.getStatus())); res.setThumbnail(p.getThumbnail());
        if (p.getCategories() != null) res.setCategoryNames(p.getCategories().stream().map(Category::getName).collect(Collectors.toList()));
        boolean isNew = p.getCreatedAt() != null && p.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7));
        res.setIsNew(isNew);
        try { Long sold = productRepository.countSoldByProduct(p.getId()); res.setIsHot(sold != null && sold >= 50); } catch (Exception e) { res.setIsHot(false); }
        boolean hasPromotion = productRepository.hasActivePromotion(p.getId(), LocalDateTime.now());
        if (hasPromotion) res.setDiscountedPrice(BigDecimal.ONE);
        if (p.getTags() != null) res.setTags(p.getTags().stream().map(ProductTag::getName).collect(Collectors.toList()));
        return res;
    }

    private ProductDetailResponse toDetailResponse(Product p) {
        ProductDetailResponse res = new ProductDetailResponse(); res.setId(p.getId()); res.setName(p.getName()); res.setSku(p.getSku()); res.setStatus(p.getStatus()); res.setThumbnail(p.getThumbnail()); res.setBrand(p.getBrand()); res.setModel(p.getModel()); res.setReleaseYear(p.getReleaseYear()); res.setGender(p.getGender()); res.setReleaseType(p.getReleaseType()); res.setLimited(p.getLimited()); res.setDescription(p.getDescription());
        if (p.getCategories() != null) { res.setCategoryIds(p.getCategories().stream().map(Category::getId).collect(Collectors.toList())); res.setCategoryNames(p.getCategories().stream().map(Category::getName).collect(Collectors.toList())); }
        if (p.getImages() != null) res.setImages(p.getImages().stream().map(img -> { ProductImageResponse imgRes = new ProductImageResponse(); imgRes.setId(img.getId()); imgRes.setUrl(img.getImageUrl()); imgRes.setThumbnail(img.isThumbnail()); return imgRes; }).collect(Collectors.toList()));
        if (p.getVariants() != null) res.setVariants(p.getVariants().stream().map(v -> { ProductVariantResponse vRes = new ProductVariantResponse(); vRes.setId(v.getId()); vRes.setSku(v.getSku()); vRes.setSize(v.getSize() != null ? v.getSize().getName() : null); vRes.setColorway(v.getColor() != null ? v.getColor().getName() : null); vRes.setMaterial(v.getMaterial() != null ? v.getMaterial().getName() : null); vRes.setSole(v.getSole() != null ? v.getSole().getName() : null); vRes.setImageUrl(v.getImageUrl()); vRes.setStock(v.getStock()); BigDecimal activePrice = productPriceRepository.findByVariant_IdAndEndDateIsNull(v.getId()).map(ProductPrice::getPrice).orElse(v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO); vRes.setPrice(activePrice); return vRes; }).collect(Collectors.toList()));
        return res;
    }

    public List<ProductSimpleResponse> getAll() { return productRepository.findAllSimpleWithVariantCount(); }
    public List<ProductSimpleResponse> getAllForPromotionEdit(Long promotionId) { return productRepository.findAllSimpleForPromotionEdit(promotionId); }
    public List<VariantResponse> getVariantsByProduct(Long productId) { return productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found")).getVariants().stream().map(v -> { VariantResponse vr = new VariantResponse(); vr.setVariantId(v.getId()); vr.setSku(v.getSku()); vr.setSize(v.getSize() != null ? v.getSize().getName() : null); vr.setColorway(v.getColor() != null ? v.getColor().getName() : null); vr.setStock(v.getStock()); BigDecimal activePrice = productPriceRepository.findByVariant_IdAndEndDateIsNull(v.getId()).map(ProductPrice::getPrice).orElse(BigDecimal.ZERO); vr.setPrice(activePrice); return vr; }).collect(Collectors.toList()); }
    private String generateVariantSku(Product product, VariantRequest v) { StringBuilder sku = new StringBuilder(product.getSku() + "-" + v.getColorway()); if (v.getMaterial() != null && !v.getMaterial().isBlank()) sku.append("-").append(v.getMaterial()); if (v.getSole() != null && !v.getSole().isBlank()) sku.append("-").append(v.getSole()); sku.append("-").append(v.getSize()); return sku.toString().toUpperCase().replace(" ", ""); }
    private void updateProductStatus(Product product) { if (product.getVariants() == null || product.getVariants().isEmpty()) return; boolean hasStock = product.getVariants().stream().anyMatch(v -> v.getStock() > 0); product.setStatus(hasStock ? "Còn hàng" : "Hết hàng"); }
    public Page<Product> getProductsByPromotion(Long promotionId, Pageable pageable) { return productRepository.findProductsByPromotion(promotionId, pageable); }
    @Transactional public void addTagToProduct(Long productId, Long tagId) { Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found")); ProductTag tag = productTagRepository.findById(tagId).orElseThrow(() -> new RuntimeException("Not found")); if (product.getTags() == null) product.setTags(new ArrayList<>()); product.getTags().add(tag); productRepository.save(product); }
    @Transactional public void updateProductTags(Long productId, List<Long> tagIds) { Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found")); List<ProductTag> tags = productTagRepository.findAllById(tagIds); product.setTags(tags); productRepository.save(product); }
    @Transactional public void removeTagFromProduct(Long productId, Long tagId) { Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found")); product.getTags().removeIf(tag -> tag.getId().equals(tagId)); productRepository.save(product); }
    public Page<ProductResponse> getProductsByTag(String tagName, Pageable pageable) { Page<Product> products = productRepository.findProductsInStock(pageable); List<ProductResponse> filtered = products.stream().map(this::toListResponse).filter(p -> { if ("NEW".equalsIgnoreCase(tagName)) return Boolean.TRUE.equals(p.getIsNew()); if ("HOT".equalsIgnoreCase(tagName)) return Boolean.TRUE.equals(p.getIsHot()); if ("SALE".equalsIgnoreCase(tagName)) return p.getDiscountedPrice() != null; return false; }).toList(); return new PageImpl<>(filtered, pageable, filtered.size()); }
    public Page<ProductResponse> getProductsByCreatedDate(LocalDate date, Pageable pageable) { return productRepository.findProductsByCreatedDate(date.atStartOfDay(), date.plusDays(1).atStartOfDay(), pageable).map(this::toListResponse); }
    public Page<ProductResponse> filterProductsByDate(LocalDate startDate, LocalDate endDate, Pageable pageable) { return productRepository.findProductsByDateRange(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), pageable).map(this::toListResponse); }
    @Transactional public ProductResponse updateStatus(Long productId, String status) { Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Not found")); product.setStatus(status); productRepository.save(product); return toListResponse(product); }
    @Transactional public void batchUpdateStatus(List<Long> ids, String status) { List<Product> products = productRepository.findAllById(ids); for (Product product : products) { product.setStatus(status); } productRepository.saveAll(products); }
    public Page<Product> getUpdatedProducts(int page, int size) { return productRepository.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, size)); }
    @Transactional public void batchDelete(List<Long> ids) { List<Product> products = productRepository.findAllById(ids); for (Product product : products) { product.setDeleted(true); product.setStatus("Ngừng bán"); if (product.getVariants() != null) { for (ProductVariant variant : product.getVariants()) { variant.setStatus("Ngừng bán"); variant.setStock(0); if (variant.getProductPrices() != null) { for (ProductPrice price : variant.getProductPrices()) { if (price.getEndDate() == null) price.setEndDate(LocalDateTime.now()); } } } } } productRepository.saveAll(products); }
    public List<ProductHistoryResponse> getProductHistory(Long productId) { return productHistoryRepository.findByProductIdOrderByUpdatedAtDesc(productId).stream().map(h -> { ProductHistoryResponse res = new ProductHistoryResponse(); res.setFieldName(h.getFieldName()); res.setOldValue(h.getOldValue()); res.setNewValue(h.getNewValue()); res.setUpdatedAt(h.getUpdatedAt()); return res; }).toList(); }
    private void saveHistory(Long productId, String field, Object oldValue, Object newValue) { if (oldValue == null && newValue == null) return; if (oldValue != null && oldValue.equals(newValue)) return; ProductHistory history = new ProductHistory(); history.setProductId(productId); history.setFieldName(field); history.setOldValue(oldValue != null ? oldValue.toString() : null); history.setNewValue(newValue != null ? newValue.toString() : null); history.setUpdatedAt(LocalDateTime.now()); productHistoryRepository.save(history); }

    private Size mapSize(String val) { if (val == null || val.trim().isEmpty()) return null; String v = val.trim(); List<Size> list = entityManager.createQuery("SELECT x FROM Size x WHERE x.name = :n", Size.class).setParameter("n", v).getResultList(); if (!list.isEmpty()) return list.get(0); Size s = new Size(); s.setName(v); entityManager.persist(s); return s; }
    private Color mapColor(String val) { if (val == null || val.trim().isEmpty()) return null; String v = val.trim(); List<Color> list = entityManager.createQuery("SELECT x FROM Color x WHERE x.name = :n", Color.class).setParameter("n", v).getResultList(); if (!list.isEmpty()) return list.get(0); Color c = new Color(); c.setName(v); entityManager.persist(c); return c; }
    private Material mapMaterial(String val) { if (val == null || val.trim().isEmpty()) return null; String v = val.trim(); List<Material> list = entityManager.createQuery("SELECT x FROM Material x WHERE x.name = :n", Material.class).setParameter("n", v).getResultList(); if (!list.isEmpty()) return list.get(0); Material m = new Material(); m.setName(v); entityManager.persist(m); return m; }
    private Sole mapSole(String val) { if (val == null || val.trim().isEmpty()) return null; String v = val.trim(); List<Sole> list = entityManager.createQuery("SELECT x FROM Sole x WHERE x.name = :n", Sole.class).setParameter("n", v).getResultList(); if (!list.isEmpty()) return list.get(0); Sole s = new Sole(); s.setName(v); entityManager.persist(s); return s; }
    private boolean matchSize(Size s, String req) { if (s == null && (req == null || req.trim().isEmpty())) return true; if (s == null || req == null || req.trim().isEmpty()) return false; return s.getName().equalsIgnoreCase(req.trim()); }
    private boolean matchColor(Color c, String req) { if (c == null && (req == null || req.trim().isEmpty())) return true; if (c == null || req == null || req.trim().isEmpty()) return false; return c.getName().equalsIgnoreCase(req.trim()); }
    private boolean matchMaterial(Material m, String req) { if (m == null && (req == null || req.trim().isEmpty())) return true; if (m == null || req == null || req.trim().isEmpty()) return false; return m.getName().equalsIgnoreCase(req.trim()); }
    private boolean matchSole(Sole s, String req) { if (s == null && (req == null || req.trim().isEmpty())) return true; if (s == null || req == null || req.trim().isEmpty()) return false; return s.getName().equalsIgnoreCase(req.trim()); }
}