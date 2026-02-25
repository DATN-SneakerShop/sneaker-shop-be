package com.sneakershop.backend.entity.customer;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "khach_hang")
@Data
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "so_dien_thoai", nullable = false, unique = true, length = 20)
    private String soDienThoai;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "loai_khach", length = 20)
    private String loaiKhach = "NORMAL"; // VIP | NORMAL

    @Column(name = "trang_thai", length = 20)
    private String status = "ACTIVE";

    @Column(name = "tao_luc", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
