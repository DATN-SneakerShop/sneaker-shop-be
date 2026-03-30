package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);

    // ĐÃ XÓA cái hàm existsByProduct_IdAndSizeAndColorway cũ gây nổ Server
}