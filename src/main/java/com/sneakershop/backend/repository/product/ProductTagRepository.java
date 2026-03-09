package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
}