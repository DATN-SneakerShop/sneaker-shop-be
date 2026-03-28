package com.sneakershop.backend.repository.voucher;

import com.sneakershop.backend.dto.voucher.CustomerVoucherDTO;
import com.sneakershop.backend.entity.voucher.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    // Lọc theo trạng thái (optional)
    List<Voucher> findByStatus(String status);

    boolean existsByName(String name);

    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' " +
            "AND v.startDate <= :now AND v.endDate >= :now " +
            "AND v.usedCount < v.quantity " +
            "AND (v.isPublic = true OR EXISTS (SELECT vc FROM VoucherCustomer vc WHERE vc.voucher.id = v.id AND vc.customer.id = :customerId))")
    List<Voucher> findAvailableVouchers(@Param("now") LocalDateTime now, @Param("customerId") Long customerId);

    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' " +
            "AND :now BETWEEN v.startDate AND v.endDate " +
            "AND v.deleted = false")
    List<Voucher> findActiveVouchersForReport(@Param("now") LocalDateTime now);
}