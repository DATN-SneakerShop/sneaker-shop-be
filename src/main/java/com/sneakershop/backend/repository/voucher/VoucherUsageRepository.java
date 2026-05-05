package com.sneakershop.backend.repository.voucher;

import com.sneakershop.backend.entity.voucher.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    boolean existsByVoucher_IdAndCustomer_Id(Long voucherId, Long customerId);
    boolean existsByVoucher_IdAndGuestEmail(Long voucherId, String guestEmail);
    boolean existsByVoucher_IdAndOrderId(Long voucherId, Long orderId);
}
