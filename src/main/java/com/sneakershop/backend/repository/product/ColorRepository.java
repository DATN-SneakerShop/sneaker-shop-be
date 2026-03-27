package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ColorRepository extends JpaRepository<Color, Long> {
    // Sắp xếp mới nhất lên đầu giống Category
    List<Color> findAllByOrderByIdDesc();
}