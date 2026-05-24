package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 🔥 THÊM: Để CategoryService gọi được hàm sắp xếp mới nhất
    List<Category> findAllByOrderByIdDesc();

    @Query("select count(e) > 0 from Category e where lower(trim(e.name)) = lower(trim(:name))")
    boolean existsByNameNormalized(@Param("name") String name);

    @Query("select count(e) > 0 from Category e where lower(trim(e.name)) = lower(trim(:name)) and e.id <> :id")
    boolean existsByNameNormalizedAndIdNot(@Param("name") String name, @Param("id") Long id);

}