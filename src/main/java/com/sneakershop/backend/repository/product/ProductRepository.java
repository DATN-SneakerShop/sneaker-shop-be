package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.dto.product.ProductSimpleResponse;
import com.sneakershop.backend.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    @Query("select distinct p from Product p left join fetch p.categories where p.id = :id")
    Optional<Product> findDetailById(@Param("id") Long id);

    // ✅ FIX: Khớp tham số cho hàm searchProducts trong Controller (image_8c4b27.jpg)
    @Query("""
        select p
        from Product p
        join p.categories c
        where
            (:keyword is null or lower(p.name) like lower(concat('%', :keyword, '%')))
            and c.id in :categoryIds
        group by p.id
        having count(distinct c.id) = :categoryCount
    """)
    Page<Product> searchProducts(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("keyword") String keyword,
            @Param("categoryCount") long categoryCount,
            Pageable pageable
    );

    @Query("""
        select new com.sneakershop.backend.dto.product.ProductSimpleResponse(
            p.id, p.name, p.brand, p.thumbnail, count(v.id), 0L
        )
        from Product p
        left join p.variants v
        group by p.id, p.name, p.brand, p.thumbnail
        ORDER BY p.id DESC
    """)
    List<ProductSimpleResponse> findAllSimpleWithVariantCount();

    @Query("""
        select new com.sneakershop.backend.dto.product.ProductSimpleResponse(
            p.id, p.name, p.brand, p.thumbnail, count(distinct v.id), count(distinct pv.id)
        )
        from Product p
        left join p.variants v
        left join v.promotions pv with pv.id = :promotionId
        group by p.id, p.name, p.brand, p.thumbnail
        ORDER BY p.id DESC
    """)
    List<ProductSimpleResponse> findAllSimpleForPromotionEdit(@Param("promotionId") Long promotionId);
}