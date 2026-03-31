package com.sneakershop.backend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sneakershop.backend.entity.promotion.PromotionDetail;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;

import lombok.Getter;
import lombok.Setter;

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
                @UniqueConstraint(
                        // 🔥 ĐÃ FIX: Sửa lại tên cột cho khớp với Khóa Ngoại (FK) bên dưới
                        columnNames = {"product_id", "size_id", "color_id", "material_id", "sole_id"}
                )
        }
)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    // ==========================================
    // 🔥 LIÊN KẾT KHÓA NGOẠI (FK) CHUẨN CHỈ
    // ==========================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id")
    private Size size;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sole_id")
    private Sole sole;

    // ==========================================

    @Column(name = "image_url")
    private String imageUrl;

    private int stock;
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

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProductPrice> productPrices;
}