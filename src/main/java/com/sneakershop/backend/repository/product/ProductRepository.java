    package com.sneakershop.backend.repository.product;

    import com.sneakershop.backend.dto.product.ProductSimpleResponse;
    import com.sneakershop.backend.entity.product.Product;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.jpa.repository.*;
    import org.springframework.data.repository.query.Param;
    import com.sneakershop.backend.entity.order.OrderItem;

    import java.util.List;
    import java.util.Optional;

    public interface ProductRepository
            extends JpaRepository<Product, Long>,
            JpaSpecificationExecutor<Product> {

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
    }
