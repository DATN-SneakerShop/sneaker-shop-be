package com.sneakershop.backend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sneakershop.backend.entity.promotion.PromotionDetail;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;

import lombok.Getter;
import lombok.Setter;

// Import 2 thư viện này để dùng Soft Delete
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Getter
@Setter
@Table(
        name = "product_variant",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "sku"),
                @UniqueConstraint(columnNames = {"product_id", "size_id", "color_id"})
        }
)
// 🔥 BẬT CƠ CHẾ XÓA MỀM (SOFT DELETE)
@SQLDelete(sql = "UPDATE product_variant SET is_deleted = true WHERE id=?")
@Where(clause = "is_deleted = false") // Tự động ẩn các bản ghi đã xóa khi lấy dữ liệu
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    // 🔥 TRƯỜNG MỚI: Đánh dấu xóa mềm
    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    // ==========================================
    // LIÊN KẾT KHÓA NGOẠI (FK)
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id")
    private Size size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    // ==========================================

    @Column(name = "image_url")
    private String imageUrl;

    private int stock;

    // 🔥 Set giá trị mặc định là 0 ở cả tầng Java và Database
    @Column(name = "reserved_quantity", columnDefinition = "int default 0")
    private int reserved_quantity = 0;

    private String status;

    @Column
    private BigDecimal price;

    @Column
    private BigDecimal salePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PromotionDetail> promotionDetails;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<VariantPriceGroup> variantPriceGroups;

    // ❌ ĐÃ XÓA: private List<ProductPrice> productPrices;
}