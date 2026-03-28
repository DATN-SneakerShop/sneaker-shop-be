package com.sneakershop.backend.entity.customer;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "khach_hang")
@Data
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    // nullable sửa thành true
    @Column(name = "email", nullable = true, unique = true, length = 100)
    private String email;

    @Column(name = "so_dien_thoai", length = 15)
    private String phone;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "loai_khach", length = 20)
    private String loaiKhach = "NORMAL";

    @Column(name = "trang_thai", length = 20)
    private String status = "ACTIVE";

    @Column(name = "tao_luc", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "diem_tich_luy")
    private Integer diemTichLuy = 0;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;

    @Column(name = "uu_dai_theo_diem")
    private Integer uuDaiTheoDiem = 0;

    @Column(name = "uu_dai_theo_nhom")
    private Integer uuDaiTheoNhom = 0;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}