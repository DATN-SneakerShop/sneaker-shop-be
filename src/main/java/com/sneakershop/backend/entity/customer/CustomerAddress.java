package com.sneakershop.backend.entity.customer;

import lombok.Data;
import javax.persistence.*;

@Entity
@Table(name = "dia_chi_khach_hang")
@Data
public class CustomerAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nhan")
    private String label;

    @Column(name = "ten_nguoi_nhan")
    private String recipientName;

    @Column(name = "so_dien_thoai")
    private String phone;

    @Column(name = "tinh_thanh_pho")
    private String province;

    @Column(name = "quan_huyen")
    private String district;

    @Column(name = "phuong_xa")
    private String ward;

    @Column(name = "dia_chi_chi_tiet")
    private String detailAddress;

    @Column(name = "mac_dinh")
    private Integer isDefault;

    // Liên kết với bảng Khách Hàng
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;
}