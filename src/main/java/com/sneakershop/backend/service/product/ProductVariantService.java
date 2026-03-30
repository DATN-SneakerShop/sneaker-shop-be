package com.sneakershop.backend.service.product;

import com.sneakershop.backend.dto.product.VariantRequest;
import com.sneakershop.backend.entity.product.Product;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.product.Size;
import com.sneakershop.backend.entity.product.Color;
import com.sneakershop.backend.entity.product.Material;
import com.sneakershop.backend.entity.product.Sole;
import com.sneakershop.backend.repository.product.ProductRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    /* ================== CREATE ================== */
    @Transactional
    public ProductVariant create(Long productId, VariantRequest request) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        // 🔥 ĐÃ FIX: Validate trùng lặp linh hoạt (Không bị phụ thuộc Repository cũ)
        boolean exists = product.getVariants().stream()
                .anyMatch(variant -> matchSize(variant.getSize(), request.getSize())
                        && matchColor(variant.getColor(), request.getColorway()));

        if (exists) {
            throw new IllegalArgumentException("Variant already exists (size + colorway)");
        }

        ProductVariant v = new ProductVariant();
        v.setProduct(product);

        // 🔥 ĐÃ FIX: Chuyển đổi an toàn
        v.setSize(mapSize(request.getSize()));
        v.setColor(mapColor(request.getColorway()));
        v.setMaterial(mapMaterial(request.getMaterial()));
        v.setSole(mapSole(request.getSole()));

        v.setStock(request.getStock());
        v.setPrice(request.getPrice());
        v.setSalePrice(request.getSalePrice());
        v.setStatus(request.getStock() > 0 ? "Còn_Hàng" : "Hết_Hàng");

        // ✅ SKU generate / validate
        if (request.getSku() != null) {
            if (variantRepository.existsBySku(request.getSku())) {
                throw new IllegalArgumentException("Variant SKU already exists: " + request.getSku());
            }
            v.setSku(request.getSku());
        }
        return variantRepository.save(v);
    }

    /* ================== UPDATE ================== */
    @Transactional
    public ProductVariant update(Long productId, Long variantId, VariantRequest request) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + variantId));

        if (!v.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Variant does not belong to product: " + productId);
        }

        v.setStock(request.getStock());
        v.setStatus(request.getStock() > 0 ? "Còn Hàng" : "Hết Hàng");
        return variantRepository.save(v);
    }

    /* ================== DELETE ================== */
    @Transactional
    public void delete(Long productId, Long variantId) {
        ProductVariant v = variantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Variant not found: " + variantId));

        if (!v.getProduct().getId().equals(productId)) {
            throw new IllegalArgumentException("Variant does not belong to product: " + productId);
        }

        variantRepository.delete(v);
    }

    // --- CÁC HÀM HELPER CHUYỂN ĐỔI ---
    private Size mapSize(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        Size s = new Size();
        try { s.setId(Long.parseLong(val)); } catch (Exception e) { s.setName(val); }
        return s;
    }
    private Color mapColor(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        Color c = new Color();
        try { c.setId(Long.parseLong(val)); } catch (Exception e) { c.setName(val); }
        return c;
    }
    private Material mapMaterial(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        Material m = new Material();
        try { m.setId(Long.parseLong(val)); } catch (Exception e) { m.setName(val); }
        return m;
    }
    private Sole mapSole(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        Sole s = new Sole();
        try { s.setId(Long.parseLong(val)); } catch (Exception e) { s.setName(val); }
        return s;
    }
    private boolean matchSize(Size s, String req) {
        if (s == null && (req == null || req.trim().isEmpty())) return true;
        if (s == null || req == null || req.trim().isEmpty()) return false;
        try { return s.getId().equals(Long.parseLong(req)); } catch (Exception e) { return req.equalsIgnoreCase(s.getName()); }
    }
    private boolean matchColor(Color c, String req) {
        if (c == null && (req == null || req.trim().isEmpty())) return true;
        if (c == null || req == null || req.trim().isEmpty()) return false;
        try { return c.getId().equals(Long.parseLong(req)); } catch (Exception e) { return req.equalsIgnoreCase(c.getName()); }
    }
}