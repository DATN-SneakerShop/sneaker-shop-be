package com.sneakershop.backend.repository.promotion;

import com.sneakershop.backend.entity.promotion.PromotionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionDetailRepository extends JpaRepository<PromotionDetail, Long> {

    @Query("""
        select pd
        from PromotionDetail pd
        join fetch pd.promotion p
        where pd.variant.id = :variantId
          and p.active = true
          and p.deleted = false
          and :now between p.startTime and p.endTime
    """)
    List<PromotionDetail> findAllActiveByVariantId(
            @Param("variantId") Long variantId,
            @Param("now") LocalDateTime now
    );
}