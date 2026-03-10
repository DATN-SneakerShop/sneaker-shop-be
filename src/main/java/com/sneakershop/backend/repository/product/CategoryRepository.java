package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 🔥 THÊM: Để CategoryService gọi được hàm sắp xếp mới nhất
    List<Category> findAllByOrderByIdDesc();
}