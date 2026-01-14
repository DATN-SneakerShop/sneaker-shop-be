package com.sneakershop.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vai_tro")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma", unique = true, nullable = false)
    private String code;

    @Column(name = "ten", nullable = false)
    private String name;

    @Column(name = "tao_luc")
    private LocalDateTime createdAt = LocalDateTime.now();
}