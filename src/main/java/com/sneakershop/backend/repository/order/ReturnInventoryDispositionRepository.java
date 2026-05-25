package com.sneakershop.backend.repository.order;

import com.sneakershop.backend.entity.order.ReturnInventoryDisposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnInventoryDispositionRepository extends JpaRepository<ReturnInventoryDisposition, Long> {
    List<ReturnInventoryDisposition> findAllByReturnRequest_IdOrderByIdAsc(Long returnRequestId);
    boolean existsByReturnRequest_Id(Long returnRequestId);
}
