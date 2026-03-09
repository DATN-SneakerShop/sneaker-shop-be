package com.sneakershop.backend.repository.product;

import com.sneakershop.backend.entity.product.ProductHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductHistoryRepository extends JpaRepository<ProductHistory, Long> {

    List<ProductHistory> findByProductIdOrderByUpdatedAtDesc(Long productId);

}