package com.sneakershop.backend.entity.product;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
@Table(name = "product_image")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;

    private boolean isThumbnail;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
}
