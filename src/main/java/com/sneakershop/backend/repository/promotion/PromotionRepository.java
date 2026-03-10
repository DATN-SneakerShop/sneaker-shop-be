package com.sneakershop.backend.repository.promotion;

import com.sneakershop.backend.entity.promotion.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    // 🔥 FIX 1: Đã gắn thêm @Param để Spring Boot map đúng dữ liệu
    @Query("""
        SELECT DISTINCT p
        FROM Promotion p
        JOIN p.variants v
        WHERE v.id = :variantId
          AND p.active = true
          AND :now BETWEEN p.startTime AND p.endTime
    """)
    List<Promotion> findAllActivePromotionsByVariant(
            @Param("variantId") Long variantId,
            @Param("now") LocalDateTime now
    );

    @Query(value = """
        SELECT MAX(CAST(SUBSTRING(code, 3) AS UNSIGNED))
        FROM promotion
        WHERE code LIKE 'DG%'
    """, nativeQuery = true)
    Integer findMaxCodeNumber();

    boolean existsByNameIgnoreCase(String name);

    // 🔥 FIX 2: Gắn thêm @Param cho đồng bộ
    @Query("""
    SELECT p
    FROM Promotion p
    JOIN p.variants v
    WHERE v.id = :variantId
      AND p.active = true
      AND p.deleted = false
      AND :now BETWEEN p.startTime AND p.endTime
    """)
    List<Promotion> findActivePromotions(
            @Param("variantId") Long variantId,
            @Param("now") LocalDateTime now);
}