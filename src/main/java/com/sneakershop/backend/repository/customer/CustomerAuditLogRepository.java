package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.CustomerAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAuditLogRepository
        extends JpaRepository<CustomerAuditLog, Long> {

    // 🔥 Lấy log theo customerId và sắp xếp theo thời gian giảm dần
    List<CustomerAuditLog> findByCustomerIdOrderByTimestampDesc(Long customerId);
}
