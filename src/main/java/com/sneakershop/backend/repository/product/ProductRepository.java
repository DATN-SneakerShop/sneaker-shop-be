    package com.sneakershop.backend.repository.product;

    import com.sneakershop.backend.dto.product.ProductSimpleResponse;
    import com.sneakershop.backend.entity.product.Product;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.*;
    import org.springframework.data.repository.query.Param;
    import com.sneakershop.backend.entity.order.OrderItem; // thêm dòng này

    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Optional;

    public interface ProductRepository
            extends JpaRepository<Product, Long>,
            JpaSpecificationExecutor<Product> {
        Page<Product> findAllByOrderByUpdatedAtDesc(Pageable pageable);
        boolean existsBySku(String sku);

        /* ================== DETAIL FETCH ================== */
        @Query("""
            select distinct p from Product p
            left join fetch p.categories
            where p.id = :id
        """)
        Optional<Product> findDetailById(@Param("id") Long id);


        /* ================== SEARCH (AND CATEGORY + KEYWORD) ================== */
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
    select p from Product p
    left join p.variants v
    left join OrderItem oi on oi.variant.id = v.id
    group by p.id
    order by coalesce(sum(oi.quantity), 0) desc
""")
        Page<Product> findBestSellingProducts(Pageable pageable);
        /* ================== YOUR PART - PROMOTION ================== */

        @Query("""
    select new com.sneakershop.backend.dto.product.ProductSimpleResponse(
        p.id,
        p.name,
        p.brand,
        p.thumbnail,
        count(v.id),
        0L
    )
    from Product p
    left join p.variants v
    group by p.id, p.name, p.brand, p.thumbnail
""")
        List<ProductSimpleResponse> findAllSimpleWithVariantCount();

        @Query("""
    select new com.sneakershop.backend.dto.product.ProductSimpleResponse(
        p.id,
        p.name,
        p.brand,
        p.thumbnail,
        count(distinct v.id),
        count(distinct pv.id)
    )
    from Product p
    left join p.variants v
    left join v.promotions pv
        with pv.id = :promotionId
    group by p.id, p.name, p.brand, p.thumbnail
""")
        List<ProductSimpleResponse> findAllSimpleForPromotionEdit(
                @Param("promotionId") Long promotionId
        );
        @Query("""
    select distinct p
    from Product p
    join p.variants v
    where v.stock > 0
""")
        Page<Product> findProductsInStock(Pageable pageable);
        @Query("""
    select distinct p
    from Product p
    join p.variants v
    join v.promotions pr
    where pr.id = :promotionId
""")
        Page<Product> findProductsByPromotion(
                @Param("promotionId") Long promotionId,
                Pageable pageable
        );
        @Query("""
    select count(pr) > 0
    from Product p
    join p.variants v
    join v.promotions pr
    where p.id = :productId
      and pr.active = true
      and pr.deleted = false
      and :now between pr.startTime and pr.endTime
""")
        boolean hasActivePromotion(
                @Param("productId") Long productId,
                @Param("now") LocalDateTime now
        );
        @Query("""
    select coalesce(sum(oi.quantity),0)
    from OrderItem oi
    where oi.variant.product.id = :productId
""")
        Long countSoldByProduct(@Param("productId") Long productId);
        @Query("""
    select p
    from Product p
    join p.variants v
    where v.stock > 0
    and p.createdAt >= :start
    and p.createdAt < :end
    group by p.id
    order by p.createdAt desc
""")
        Page<Product> findProductsByCreatedDate(
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end,
                Pageable pageable
        );
        @Query("""
select p
from Product p
join p.variants v
where v.stock > 0
and p.createdAt >= :start
and p.createdAt <= :end
group by p.id
order by p.createdAt desc
""")
        Page<Product> findProductsByDateRange(
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end,
                Pageable pageable
        );
    }


