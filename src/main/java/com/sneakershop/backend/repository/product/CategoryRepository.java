package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Dùng findAllById(...) của JpaRepository là đủ
}
