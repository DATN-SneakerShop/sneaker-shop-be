package com.sneakershop.backend.entity.pricing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tien_te")
@Getter
@Setter
public class TienTe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false, unique = true)
    private String ma;        // VND, USD

    @Column(length = 50, nullable = false)
    private String ten;       // Việt Nam Đồng

    @Column(length = 10, nullable = false)
    private String kyHieu;    // ₫, $

    @Column(name = "la_mac_dinh")
    private Boolean laMacDinh = false;
}
