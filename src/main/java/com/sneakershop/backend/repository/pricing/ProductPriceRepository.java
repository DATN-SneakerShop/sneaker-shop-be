package com.sneakershop.backend.repository.pricing;

import com.sneakershop.backend.dto.pricing.PriceBoardDTO;
import com.sneakershop.backend.dto.pricing.PriceHistoryDTO;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {

    /**
     * 🔥 Bảng giá hiện tại (giá đang active) - Đã chặn sản phẩm bị xóa
     */
    @Query("""
    SELECT new com.sneakershop.backend.dto.pricing.PriceBoardDTO(
        v.id, p.name, v.sku, v.colorway, v.size, pp.price, c.symbol
    )
    FROM ProductVariant v
    JOIN v.product p
    LEFT JOIN ProductPrice pp
        ON pp.variant = v AND pp.endDate IS NULL
    LEFT JOIN pp.currency c
    WHERE p.deleted = false OR p.deleted IS NULL
    ORDER BY v.id DESC
""")
    List<PriceBoardDTO> getCurrentPriceBoard();

    /**
     * 🔥 Giá đang áp dụng của variant
     */
    Optional<ProductPrice> findByVariant_IdAndEndDateIsNull(Long variantId);

    /**
     * 🔥 Lock giá active khi update (tránh update song song)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT pp FROM ProductPrice pp
        WHERE pp.variant.id = :variantId
        AND pp.endDate IS NULL
    """)
    Optional<ProductPrice> findActivePriceForUpdate(
            @Param("variantId") Long variantId
    );
    @Query("""
    SELECT new com.sneakershop.backend.dto.pricing.PriceHistoryDTO(
        pp.id,
        pp.price,
        c.symbol,
        pp.startDate,
        pp.endDate,
        CASE WHEN pp.endDate IS NULL THEN true ELSE false END
    )
    FROM ProductPrice pp
    JOIN pp.currency c
    WHERE pp.variant.id = :variantId
    ORDER BY pp.startDate DESC
""")
    List<PriceHistoryDTO> getPriceHistoryByVariant(
            @Param("variantId") Long variantId
    );
    @Query("""
    SELECT p
    FROM ProductPrice p
    WHERE p.variant.id = :variantId
      AND p.endDate IS NULL
      AND p.isDefault = true
""")
    Optional<ProductPrice> findActivePrice(Long variantId);
    Optional<ProductPrice> findFirstByVariantIdAndEndDateIsNull(Long variantId);


}
