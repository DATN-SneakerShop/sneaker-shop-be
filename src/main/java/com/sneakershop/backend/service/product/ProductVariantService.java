package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.VariantRequest;
import com.sneakershop.backend.entity.product.*;
import com.sneakershop.backend.repository.product.ProductRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import com.sneakershop.backend.exception.ValidationException;
import com.sneakershop.backend.service.ValidationSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final EntityManager entityManager; // 🔥 Bổ sung EntityManager để lấy dữ liệu khóa ngoại an toàn

    /* ================== CREATE ================== */
    @Transactional
    public ProductVariant create(Long productId, VariantRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm: " + productId));

        normalizeAndValidateVariantRequest(request, null);
        boolean exists = product.getVariants().stream()
                .anyMatch(variant -> matchSize(variant.getSize(), request.getSize())
                        && matchColor(variant.getColor(), request.getColorway()));

        if (exists) {
            throw new ValidationException("variants", "Biến thể sản phẩm này đã tồn tại.");
        }

        ProductVariant v = new ProductVariant();
        v.setProduct(product);

        // 🔥 ĐÃ FIX: Sử dụng EntityManager để tham chiếu an toàn, tránh lỗi Transient của Hibernate
        v.setSize(mapSize(request.getSize()));
        v.setColor(mapColor(request.getColorway()));

        v.setStock(request.getStock() != 0 ? request.getStock() : 0);
        v.setPrice(request.getPrice());
        v.setSalePrice(request.getSalePrice());
        v.setStatus(v.getStock() > 0 ? "Còn Hàng" : "Hết Hàng");

        // ✅ Validate SKU
        if (request.getSku() != null && !request.getSku().trim().isEmpty()) {
            if (variantRepository.existsBySkuNormalized(request.getSku())) {
                throw new ValidationException("sku", "Mã SKU biến thể đã được sử dụng.");
            }
            v.setSku(request.getSku());
        }

        // Cập nhật thêm ảnh nếu có truyền từ giao diện
        // if (request.getImageUrl() != null) v.setImageUrl(request.getImageUrl());

        return variantRepository.save(v);
    }

    /* ================== UPDATE ================== */
    @Transactional
    public ProductVariant update(Long productId, Long variantId, VariantRequest request) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy biến thể: " + variantId));

        if (!v.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Biến thể không thuộc về sản phẩm này: " + productId);
        }

        normalizeAndValidateVariantRequest(request, variantId);

        boolean comboExists = v.getProduct().getVariants().stream()
                .anyMatch(other -> !other.getId().equals(variantId)
                        && matchSize(other.getSize(), request.getSize())
                        && matchColor(other.getColor(), request.getColorway()));
        if (comboExists) {
            throw new ValidationException("variants", "Biến thể sản phẩm này đã tồn tại.");
        }

        v.setSize(mapSize(request.getSize()));
        v.setColor(mapColor(request.getColorway()));

        // 🔥 ĐÃ FIX: Bổ sung cập nhật đầy đủ các trường từ ma trận giao diện Vue.js
        if (request.getPrice() != null) v.setPrice(request.getPrice());
        if (request.getSalePrice() != null) v.setSalePrice(request.getSalePrice());
        if (request.getStock() != 0) {
            validateStockNotBelowReserved(v, request.getStock());
            v.setStock(request.getStock());
        }

        // Kiểm tra logic nếu SKU bị thay đổi
        if (request.getSku() != null && !request.getSku().trim().isEmpty() && !request.getSku().equalsIgnoreCase(v.getSku())) {
            if (variantRepository.existsBySkuNormalizedAndIdNot(request.getSku(), variantId)) {
                throw new ValidationException("sku", "Mã SKU biến thể đã được sử dụng.");
            }
            v.setSku(request.getSku());
        }

        // Cập nhật ảnh nếu có truyền từ giao diện
        // if (request.getImageUrl() != null) v.setImageUrl(request.getImageUrl());

        v.setStatus(v.getStock() > 0 ? "Còn Hàng" : "Hết Hàng");
        return variantRepository.save(v);
    }

    /* ================== DELETE ================== */
    @Transactional
    public void delete(Long productId, Long variantId) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy biến thể: " + variantId));

        if (!v.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Biến thể không thuộc về sản phẩm này: " + productId);
        }

        if (Math.max(v.getReserved_quantity(), 0) > 0) {
            throw new ValidationException("stock", "Không thể xóa biến thể đang có số lượng giữ chỗ: " + Math.max(v.getReserved_quantity(), 0) + ". Vui lòng xử lý/hủy đơn liên quan trước.");
        }

        // Nhờ @SQLDelete bên Entity, lệnh delete này sẽ tự động chạy ngầm UPDATE is_deleted=true
        variantRepository.delete(v);
    }

    private void validateStockNotBelowReserved(ProductVariant variant, int newStock) {
        int reserved = Math.max(variant.getReserved_quantity(), 0);
        if (newStock < reserved) {
            throw new ValidationException("stock", "Tồn kho mới không được nhỏ hơn số lượng đang giữ chỗ: " + reserved + ".");
        }
    }

    private void normalizeAndValidateVariantRequest(VariantRequest request, Long currentId) {
        request.setSku(ValidationSupport.trim(request.getSku()));
        request.setSize(ValidationSupport.trim(request.getSize()));
        request.setColorway(ValidationSupport.trim(request.getColorway()));
        if (request.getStock() < 0) {
            throw new ValidationException("stock", "Số lượng sản phẩm không hợp lệ.");
        }
        if (request.getSku() != null) {
            boolean duplicate = currentId == null
                    ? variantRepository.existsBySkuNormalized(request.getSku())
                    : variantRepository.existsBySkuNormalizedAndIdNot(request.getSku(), currentId);
            if (duplicate) throw new ValidationException("sku", "Mã SKU biến thể đã được sử dụng.");
        }
    }

    // ================== CÁC HÀM HELPER CHUYỂN ĐỔI ==================
    // 🔥 ĐÃ FIX: Chuẩn hóa Hibernate getReference
    private Size mapSize(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return entityManager.getReference(Size.class, Long.parseLong(val));
        } catch (NumberFormatException e) {
            Size s = new Size(); s.setName(val); return s;
        }
    }

    private Color mapColor(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return entityManager.getReference(Color.class, Long.parseLong(val));
        } catch (NumberFormatException e) {
            Color c = new Color(); c.setName(val); return c;
        }
    }

    private boolean matchSize(Size s, String req) {
        if (s == null && (req == null || req.trim().isEmpty())) return true;
        if (s == null || req == null || req.trim().isEmpty()) return false;
        try { return s.getId().equals(Long.parseLong(req)); }
        catch (NumberFormatException e) { return req.equalsIgnoreCase(s.getName()); }
    }

    private boolean matchColor(Color c, String req) {
        if (c == null && (req == null || req.trim().isEmpty())) return true;
        if (c == null || req == null || req.trim().isEmpty()) return false;
        try { return c.getId().equals(Long.parseLong(req)); }
        catch (NumberFormatException e) { return req.equalsIgnoreCase(c.getName()); }
    }
}