package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.CustomerRank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerRankRepository extends JpaRepository<CustomerRank, Long> {
    // Sắp xếp giảm dần theo điểm để dễ dàng quét hạng cho khách
    List<CustomerRank> findAllByOrderByMinPointsDesc();

    boolean existsByName(String name);
}