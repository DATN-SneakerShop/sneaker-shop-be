package com.sneakershop.backend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sneakershop.backend.entity.promotion.Promotion;
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
                        // 🔥 ĐÃ THÊM: Ràng buộc duy nhất cho 4 trục biến thể
                        columnNames = {"product_id", "size", "colorway", "material", "sole"}
                )
        }
)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(name = "size")
    private String size;

    @Column(name = "colorway")
    private String colorway;

    // 🔥 CỘT MỚI: Chất liệu và Loại đế/Phiên bản
    @Column(name = "material")
    private String material;

    @Column(name = "sole")
    private String sole;

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

    @ManyToMany(mappedBy = "variants")
    @JsonIgnore
    private List<Promotion> promotions;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProductPrice> productPrices;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<VariantPriceGroup> variantPriceGroups;
}