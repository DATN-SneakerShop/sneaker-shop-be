package com.sneakershop.backend.entity.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.promotion.Promotion;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@Table(
        name = "product_variant",
        uniqueConstraints = @UniqueConstraint(columnNames = "sku"),
        indexes = {
                @Index(name = "idx_variant_product", columnList = "product_id"),
                @Index(name = "idx_variant_sku", columnList = "sku")
        }
)
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sku;

    private String size;
    private String colorway;

    @Column(nullable = false)
    private int stock;

    private String status; // IN_STOCK / OUT_OF_STOCK

    // FK sang Product
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;


    // Lịch sử giá
    @OneToMany(mappedBy = "variant")
    @JsonIgnore
    private List<ProductPrice> prices;

    @ManyToMany(mappedBy = "variants")
    @JsonIgnore
    private List<Promotion> promotions;
}
