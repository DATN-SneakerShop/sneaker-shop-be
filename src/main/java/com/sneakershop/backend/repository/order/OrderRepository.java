package com.sneakershop.backend.repository.order;

import com.sneakershop.backend.dto.customer.CustomerHistoryDTO;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndDeletedFalse(Long id);

    List<Order> findAllByDeletedFalseOrderByCreatedAtDesc();

    List<Order> findAllByOrderStatusAndDeletedFalseOrderByCreatedAtDesc(OrderStatus orderStatus);

    List<Order> findAllByCustomer_IdAndDeletedFalseOrderByCreatedAtDesc(Long customerId);

    List<Order> findAllByCreatedBy_IdAndDeletedFalseOrderByCreatedAtDesc(Long createdById);

    List<Order> findAllByReturnStatusIsNotNullAndDeletedFalseOrderByCreatedAtDesc();

    List<Order> findAllByReturnStatusAndDeletedFalseOrderByCreatedAtDesc(ReturnStatus returnStatus);

    boolean existsByOrderCode(String orderCode);

    @Query("""
        select new com.sneakershop.backend.dto.customer.CustomerHistoryDTO(
            c.id,
            c.ten,
            c.email,
            o.id,
            o.orderCode,
            o.createdAt,
            o.finalAmount,
            o.orderStatus,
            o.paymentStatus
        )
        from Order o
        join o.customer c
        where o.deleted = false
        order by o.createdAt desc
    """)
    List<CustomerHistoryDTO> getCustomerHistory();
}