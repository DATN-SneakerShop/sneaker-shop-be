package com.sneakershop.backend.entity.product;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "product")
public class Product {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên hiển thị
    @Column(nullable = false)
    private String name;

    // SKU cha
    @Column(nullable = false, unique = true)
    private String sku;

    private String brand;
    private String model;
    private String releaseYear;

    private String gender;
    private String releaseType;
    private String status; //Còn hàng ,Hết hàng ,Đặt trước , Ngừng bán

    private String material;
    private Boolean limited;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * thumbnail URL (ảnh đại diện nhanh)
     * ví dụ: uploads/abc.jpg
     */
    private String thumbnail;

    /* ================== CATEGORIES ================== */
    @ManyToMany
    @JoinTable(
            name = "product_category",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> categories;

    /* ================== VARIANTS ================== */
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProductVariant> variants;

    /* ================== IMAGES ================== */
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonManagedReference
    private List<ProductImage> images;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
