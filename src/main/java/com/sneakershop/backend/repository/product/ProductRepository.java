package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.dto.product.ProductSimpleResponse;
import com.sneakershop.backend.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Kiểm tra xem mã SKU đã tồn tại trong hệ thống chưa
     */
    boolean existsBySku(String sku);

    /**
     * Lấy danh sách sản phẩm phân trang, sắp xếp theo thời gian cập nhật mới nhất
     */
    Page<Product> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    /**
     * Lấy chi tiết sản phẩm kèm theo danh sách categories (sử dụng fetch join để tránh lỗi N+1 query)
     */
    @Query("select distinct p from Product p left join fetch p.categories where p.id = :id")
    Optional<Product> findDetailById(@Param("id") Long id);

    /**
     * Tìm kiếm sản phẩm theo từ khóa và danh sách categories (Lọc AND - sản phẩm phải có đủ các category được truyền vào)
     * Lưu ý: Không gọi hàm này nếu categoryIds rỗng (empty), sẽ gây lỗi SQL syntax ở mệnh đề IN.
     */
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

    /**
     * Lấy danh sách sản phẩm bán chạy nhất dựa trên tổng số lượng trong các OrderItem
     */
    @Query("""
        select p from Product p
        left join p.variants v
        left join OrderItem oi on oi.variant.id = v.id
        group by p.id
        order by coalesce(sum(oi.quantity), 0) desc
    """)
    Page<Product> findBestSellingProducts(Pageable pageable);

    /**
     * Lấy danh sách sản phẩm dạng DTO rút gọn kèm theo tổng số biến thể (variants) của nó
     */
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

    /**
     * Lấy danh sách sản phẩm dạng DTO rút gọn để hiển thị trong màn hình Edit Khuyến mãi,
     * kèm theo số lượng variant đang tham gia vào promotionId được chỉ định
     */
    @Query("""
        select new com.sneakershop.backend.dto.product.ProductSimpleResponse(
            p.id, p.name, p.brand, p.thumbnail, count(distinct v.id), count(distinct pd.id)
        )
        from Product p
        left join p.variants v
        left join v.promotionDetails pd on pd.promotion.id = :promotionId
        group by p.id, p.name, p.brand, p.thumbnail
        ORDER BY p.id DESC
    """)
    List<ProductSimpleResponse> findAllSimpleForPromotionEdit(@Param("promotionId") Long promotionId);

    /**
     * Lấy danh sách các sản phẩm còn hàng (có ít nhất 1 biến thể có stock > 0)
     */
    @Query("""
        select distinct p
        from Product p
        join p.variants v
        where v.stock > 0
    """)
    Page<Product> findProductsInStock(Pageable pageable);

    /**
     * Lấy danh sách sản phẩm đang nằm trong một chương trình khuyến mãi cụ thể
     */
    @Query("""
        select distinct p
        from Product p
        join p.variants v
        join v.promotionDetails pd
        join pd.promotion pr
        where pr.id = :promotionId
    """)
    Page<Product> findProductsByPromotion(
            @Param("promotionId") Long promotionId,
            Pageable pageable
    );

    /**
     * Kiểm tra xem sản phẩm có đang trong chương trình khuyến mãi nào đang active và trong thời gian hiệu lực hay không
     */
    @Query("""
        select count(pr) > 0
        from Product p
        join p.variants v
        join v.promotionDetails pd
        join pd.promotion pr
        where p.id = :productId
          and pr.active = true
          and pr.deleted = false
          and :now between pr.startTime and pr.endTime
    """)
    boolean hasActivePromotion(
            @Param("productId") Long productId,
            @Param("now") LocalDateTime now
    );

    /**
     * Đếm tổng số lượng đã bán của một sản phẩm
     */
    @Query("""
        select coalesce(sum(oi.quantity),0)
        from OrderItem oi
        where oi.variant.product.id = :productId
    """)
    Long countSoldByProduct(@Param("productId") Long productId);

    /**
     * Tìm sản phẩm còn hàng được tạo trong khoảng thời gian cụ thể (sử dụng dấu < cho end)
     */
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

    /**
     * Tìm sản phẩm còn hàng được tạo trong khoảng thời gian cụ thể (sử dụng dấu <= cho end)
     */
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

    @Query("""
    select distinct p
    from Product p
    left join fetch p.variants v
    left join fetch v.color
    left join fetch v.size
    where (p.deleted is null or p.deleted = false)
      and (p.status is null or lower(p.status) <> lower('Ngừng bán'))
""")
    List<Product> findAllForStorefrontHome();
}