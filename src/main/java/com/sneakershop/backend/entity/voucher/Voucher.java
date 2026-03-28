package com.sneakershop.backend.entity.voucher;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Where;

@Where(clause = "deleted = false")
@Entity
@Table(name = "voucher")
@Data
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã voucher
    @Column(name = "ma", unique = true, nullable = false, length = 50)
    private String code;

    // Tên voucher
    @Column(name = "ten", length = 255)
    private String name;

    // Loại giảm giá: PERCENT / FIXED
    @Column(name = "loai_giam", nullable = false, length = 20)
    private String type;

    // Giá trị giảm
    @Column(name = "gia_tri_giam", nullable = false)
    private Long  value;

    // Giảm tối đa (chỉ áp dụng với %)
    @Column(name = "giam_toi_da")
    private Long  maxDiscount;

    // Đơn tối thiểu
    @Column(name = "don_toi_thieu")
    private Long  minOrderValue;

    // Tổng số lượng
    @Column(name = "so_luong")
    private Integer quantity;

    // Đã sử dụng
    @Column(name = "da_su_dung")
    private Integer usedCount = 0;

    // Thời gian áp dụng
    @Column(name = "bat_dau")
    private LocalDateTime startDate;

    @Column(name = "ket_thuc")
    private LocalDateTime endDate;

    // Trạng thái: ACTIVE / INACTIVE / EXPIRED
    @Column(name = "trang_thai", length = 20)
    private String status = "ACTIVE";

    // Công khai hay không
    @Column(name = "cong_khai")
    private Boolean isPublic = true;

    // Mô tả
    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String description;

    // Thời gian tạo
    @Column(name = "tao_luc", updatable = false)
    private LocalDateTime createdAt;

    // Thời gian cập nhật
    @Column(name = "cap_nhat_luc")
    private LocalDateTime updatedAt;

    // 👉 Auto set khi insert
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 👉 Auto update khi update
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}