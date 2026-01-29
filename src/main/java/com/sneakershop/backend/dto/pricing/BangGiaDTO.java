package com.sneakershop.backend.dto.pricing;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class BangGiaDTO {
    private Long sanPhamId;
    private String tenSanPham;
    private BigDecimal giaHienTai;
}
