package com.sneakershop.backend.repository.voucher;

import com.sneakershop.backend.entity.voucher.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    // Không cần viết gì thêm, JpaRepository đã hỗ trợ hàm save()
}