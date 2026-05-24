package com.sneakershop.backend.repository.promotion;

import com.sneakershop.backend.entity.promotion.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("""
        SELECT DISTINCT p
        FROM Promotion p
        JOIN p.promotionDetails pd
        JOIN pd.variant v
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

    @Query("""
        SELECT p
        FROM Promotion p
        JOIN p.promotionDetails pd
        JOIN pd.variant v
        WHERE v.id = :variantId
          AND p.active = true
          AND (p.deleted = false OR p.deleted IS NULL)
          AND :now BETWEEN p.startTime AND p.endTime
    """)
    List<Promotion> findActivePromotions(
            @Param("variantId") Long variantId,
            @Param("now") LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE p.active = true " +
            "AND :now BETWEEN p.startTime AND p.endTime " +
            "AND p.deleted = false")
    List<Promotion> findActivePromotionsForReport(@Param("now") LocalDateTime now);

    @Query("select count(p) > 0 from Promotion p where lower(trim(p.name)) = lower(trim(:name)) and (p.deleted is null or p.deleted = false)")
    boolean existsByNameNormalized(@Param("name") String name);

    @Query("select count(p) > 0 from Promotion p where lower(trim(p.name)) = lower(trim(:name)) and p.id <> :id and (p.deleted is null or p.deleted = false)")
    boolean existsByNameNormalizedAndIdNot(@Param("name") String name, @Param("id") Long id);

    @Query("""
        select count(pd) > 0 from PromotionDetail pd
        join pd.promotion p
        where pd.variant.id = :variantId
          and p.id <> :promotionId
          and (p.deleted is null or p.deleted = false)
          and p.active = true
          and p.startTime < :endTime
          and p.endTime > :startTime
    """)
    boolean existsActiveOverlapForVariant(@Param("variantId") Long variantId, @Param("startTime") java.time.LocalDateTime startTime, @Param("endTime") java.time.LocalDateTime endTime, @Param("promotionId") Long promotionId);

}