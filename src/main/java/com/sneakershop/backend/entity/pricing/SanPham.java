package com.sneakershop.backend.entity.pricing;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "san_pham")
@Getter @Setter
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tối thiểu để hiển thị giá
    @Column(name = "ten", length = 150)
    private String ten;
}

