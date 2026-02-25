package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.dto.product.ProductSimpleResponse;
import com.sneakershop.backend.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategory_Id(Long categoryId, Pageable pageable);
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
