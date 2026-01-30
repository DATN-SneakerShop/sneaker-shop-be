package com.sneakershop.backend.entity.product;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter @Setter
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // Nike Air Jordan 1 Retro High Bred 2026

    private String brand;
    private String gender;        // Nam / Nữ / Unisex
    private String releaseType;   // Retro / OG / Limited
    private String status;        // IN_STOCK / PRE_ORDER / DROP

    @Column(columnDefinition = "TEXT")
    private String description;

    private String thumbnail;     // ảnh đại diện

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductVariant> variants;
}
