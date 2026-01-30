package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
}

