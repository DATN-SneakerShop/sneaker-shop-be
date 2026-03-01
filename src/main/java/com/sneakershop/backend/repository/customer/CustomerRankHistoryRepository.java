package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.CustomerRankHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRankHistoryRepository
        extends JpaRepository<CustomerRankHistory, Long> {

    List<CustomerRankHistory> findByCustomerIdOrderByChangedAtDesc(Long customerId);
}
