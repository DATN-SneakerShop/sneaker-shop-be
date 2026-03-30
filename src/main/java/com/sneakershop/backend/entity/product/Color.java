package com.sneakershop.backend.entity.product;
import lombok.Getter;
import lombok.Setter;
import javax.persistence.*;

@Entity
@Getter @Setter
@Table(name = "color")
public class Color {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(name = "hex_code")
    private String hexCode;
    @Column(name = "deleted")
    private Boolean deleted = false; // 🔥 Thêm để Ẩn/Hiện
}