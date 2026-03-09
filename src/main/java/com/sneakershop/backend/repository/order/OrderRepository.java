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
    List<Order> findAllByOrderStatusAndDeletedFalseOrderByCreatedAtDesc(OrderStatus status);

    // Checklist: hiển thị đơn theo khách hàng / theo nhân viên bán
    List<Order> findAllByCustomer_IdAndDeletedFalseOrderByCreatedAtDesc(Long customerId);
    List<Order> findAllByCreatedBy_IdAndDeletedFalseOrderByCreatedAtDesc(Long createdById);

    // Checklist: báo cáo đơn hoàn trả
    List<Order> findAllByReturnStatusIsNotNullAndDeletedFalseOrderByCreatedAtDesc();
    List<Order> findAllByReturnStatusAndDeletedFalseOrderByCreatedAtDesc(ReturnStatus returnStatus);
    
    boolean existsByOrderCode(String orderCode);

    // Lấy chi tiêu khách hàng
    @Query("""
SELECT o.customer.id, o.customer.ten, SUM(o.finalAmount)
FROM Order o
WHERE o.customer IS NOT NULL
GROUP BY o.customer.id, o.customer.ten
ORDER BY SUM(o.finalAmount) DESC
""")
    List<Object[]> getCustomerSpending();

    // Khách hàng lâu chưa mua
    @Query("""
SELECT c.id, c.ten, MAX(o.createdAt)
FROM Order o
JOIN o.customer c
GROUP BY c.id, c.ten
""")
    List<Object[]> getLastOrderTime();


    // Lịch sử giao dịch
    @Query("""
SELECT new com.sneakershop.backend.dto.customer.CustomerHistoryDTO(
    o.id,
    o.orderCode,
    c.ten,
    o.finalAmount,
    o.createdAt
)
FROM Order o
JOIN o.customer c
WHERE o.customer IS NOT NULL
AND o.orderStatus = 'COMPLETED'
ORDER BY o.createdAt DESC
""")
    List<CustomerHistoryDTO> getCustomerHistory();
}



