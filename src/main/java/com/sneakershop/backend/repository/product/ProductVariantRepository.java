package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);

    List<ProductVariant> findByProduct_Id(Long productId);

    @Query("select count(v) > 0 from ProductVariant v where lower(trim(v.sku)) = lower(trim(:sku))")
    boolean existsBySkuNormalized(@Param("sku") String sku);

    @Query("select count(v) > 0 from ProductVariant v where lower(trim(v.sku)) = lower(trim(:sku)) and v.id <> :id")
    boolean existsBySkuNormalizedAndIdNot(@Param("sku") String sku, @Param("id") Long id);

    @Query("select count(v) > 0 from ProductVariant v where v.product.id = :productId and v.size.id = :sizeId and v.color.id = :colorId")
    boolean existsByProductSizeColor(@Param("productId") Long productId, @Param("sizeId") Long sizeId, @Param("colorId") Long colorId);

    @Query("select count(v) > 0 from ProductVariant v where v.product.id = :productId and v.size.id = :sizeId and v.color.id = :colorId and v.id <> :id")
    boolean existsByProductSizeColorAndIdNot(@Param("productId") Long productId, @Param("sizeId") Long sizeId, @Param("colorId") Long colorId, @Param("id") Long id);



    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ProductVariant v where v.id = :id")
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);
}