package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.CustomerPointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerPointHistoryRepository
        extends JpaRepository<CustomerPointHistory, Long> {

    List<CustomerPointHistory> findByCustomerIdOrderByChangedAtDesc(Long customerId);

    boolean existsByCustomerIdAndReasonContaining(Long customerId, String keyword);
}