package com.sneakershop.backend.entity.product;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "product_history")
public class ProductHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    // tên field thay đổi
    private String fieldName;

    // giá trị cũ
    @Column(columnDefinition = "TEXT")
    private String oldValue;

    // giá trị mới
    @Column(columnDefinition = "TEXT")
    private String newValue;

    private LocalDateTime updatedAt;

}