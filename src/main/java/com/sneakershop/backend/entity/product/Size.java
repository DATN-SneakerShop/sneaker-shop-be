package com.sneakershop.backend.entity.product;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter @Setter
public class Size {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name; // Ví dụ: 38, 39, 40, S, M, L
}