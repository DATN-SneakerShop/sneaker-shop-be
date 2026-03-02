package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    /* ================== VALIDATE ================== */

    // Check trùng SKU (toàn hệ thống)
    boolean existsBySku(String sku);

    // ✅ Check trùng variant trong cùng product
    boolean existsByProduct_IdAndSizeAndSizeTypeAndColorway(
            Long productId,
            String size,
            String sizeType,
            String colorway
    );

    /* ================== QUERY ================== */

    // ✅ Lấy toàn bộ variant của 1 sản phẩm
    List<ProductVariant> findByProduct_Id(Long productId);
}
