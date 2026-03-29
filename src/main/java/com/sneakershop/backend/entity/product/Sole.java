package com.sneakershop.backend.entity.product;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter @Setter
public class Sole {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name; // Ví dụ: Đế TF, Đế IC, Gore-Tex
}