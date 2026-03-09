package com.sneakershop.backend.entity.product;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.sneakershop.backend.entity.promotion.Promotion;
// 🔥 Import thêm 2 entity bảng giá
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
                        columnNames = {"product_id", "size", "sizeType", "colorway"}
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

    @Column(name = "sizeType")
    private String sizeType;

    @Column(name = "colorway")
    private String colorway;

    private int stock;
    private String status;

    @Column
    private BigDecimal price;

    @Column
    private BigDecimal salePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonBackReference
    private Product product;

    @ManyToMany(mappedBy = "variants")
    @JsonIgnore
    private List<Promotion> promotions;

    // 🔥 FIX LỖI 500: Tự động xóa dữ liệu ở bảng lịch sử giá khi xóa sản phẩm
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProductPrice> productPrices;

    // 🔥 FIX LỖI 500: Tự động xóa dữ liệu ở bảng giá nhóm khách khi xóa sản phẩm
    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<VariantPriceGroup> variantPriceGroups;
}