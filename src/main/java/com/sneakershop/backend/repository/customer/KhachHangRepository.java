package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {

    List<KhachHang> findByStatus(String status);
}
