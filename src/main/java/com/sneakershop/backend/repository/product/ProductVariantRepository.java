package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    @Query("""
        select v
        from ProductVariant v
        where v.product.id = :productId
    """)
    List<ProductVariant> findByProductId(Long productId);
}

