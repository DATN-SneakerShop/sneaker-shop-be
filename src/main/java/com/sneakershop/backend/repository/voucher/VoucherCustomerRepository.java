package com.sneakershop.backend.repository.voucher;

import com.sneakershop.backend.entity.voucher.VoucherCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoucherCustomerRepository extends JpaRepository<VoucherCustomer, Long> {

    void deleteByVoucherId(Long voucherId);

    List<VoucherCustomer> findByVoucherId(Long voucherId);

    boolean existsByVoucher_IdAndCustomer_Id(Long voucherId, Long customerId);
}