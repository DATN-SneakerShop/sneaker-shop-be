package com.sneakershop.backend.repository.pricing;


import com.sneakershop.backend.entity.pricing.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long> {
}