package com.sneakershop.backend.repository.order;

import com.sneakershop.backend.entity.order.ReturnRequest;
import com.sneakershop.backend.entity.order.enums.ReturnRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findByIdAndOrder_DeletedFalse(Long id);

    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    List<ReturnRequest> findAllByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    List<ReturnRequest> findAllByStatusOrderByCreatedAtDesc(ReturnRequestStatus status);

    boolean existsByCode(String code);

    boolean existsByOrder_IdAndStatusIn(Long orderId, Collection<ReturnRequestStatus> statuses);

    @Query("""
        select coalesce(sum(i.quantity), 0)
        from ReturnRequest r join r.items i
        where i.orderItem.id = :orderItemId
          and r.status in :statuses
    """)
    Long sumReturnedQuantityByOrderItemIdAndStatuses(@Param("orderItemId") Long orderItemId,
                                                        @Param("statuses") Collection<ReturnRequestStatus> statuses);
}
