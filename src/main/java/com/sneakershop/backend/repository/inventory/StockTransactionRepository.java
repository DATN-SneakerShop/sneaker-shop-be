package com.sneakershop.backend.repository.inventory;

import com.sneakershop.backend.entity.inventory.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    List<StockTransaction> findByVariant_IdOrderByCreatedAtDesc(Long variantId);
    List<StockTransaction> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(String referenceType, Long referenceId);
}
