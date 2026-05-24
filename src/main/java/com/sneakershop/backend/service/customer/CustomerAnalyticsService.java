package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.dto.customer.CustomerDashboardDTO;
import com.sneakershop.backend.dto.customer.CustomerSpendingDTO;
import com.sneakershop.backend.dto.customer.CustomerTransactionDTO;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAnalyticsService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<CustomerSpendingDTO> spending() {
        Map<Long, CustomerSpendingDTO> result = new LinkedHashMap<>();

        for (Customer c : customerRepository.findByStatus("ACTIVE")) {
            result.put(c.getId(), baseSpending(c));
        }

        for (Order order : orderRepository.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            Customer customer = order.getCustomer();
            if (customer == null || customer.getId() == null) continue;
            if (!isRevenueOrder(order)) continue;

            CustomerSpendingDTO dto = result.computeIfAbsent(customer.getId(), id -> baseSpending(customer));
            BigDecimal netAmount = netRevenue(order);
            dto.setTotalSpent(nz(dto.getTotalSpent()).add(netAmount));
            dto.setOrderCount((dto.getOrderCount() == null ? 0L : dto.getOrderCount()) + 1L);

            LocalDateTime revenueDate = revenueDate(order);
            if (dto.getLastOrderAt() == null || (revenueDate != null && revenueDate.isAfter(dto.getLastOrderAt()))) {
                dto.setLastOrderAt(revenueDate);
            }
        }

        return result.values().stream()
                .sorted(Comparator.comparing(CustomerSpendingDTO::getTotalSpent, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerSpendingDTO> topCustomers(int limit) {
        return spending().stream()
                .filter(i -> nz(i.getTotalSpent()).compareTo(BigDecimal.ZERO) > 0)
                .limit(Math.max(1, limit))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerSpendingDTO> vipCustomers(Integer minPoint, Integer maxPoint) {
        int min = minPoint == null ? 0 : minPoint;
        int max = maxPoint == null ? Integer.MAX_VALUE : maxPoint;
        return spending().stream()
                .filter(c -> safePoint(c) >= min && safePoint(c) <= max)
                .filter(c -> safePoint(c) > 0 || isVipRank(c.getRankName()))
                .sorted(Comparator.comparing(CustomerSpendingDTO::getPoint, Comparator.nullsLast(Integer::compareTo)).reversed()
                        .thenComparing(CustomerSpendingDTO::getTotalSpent, Comparator.nullsLast(BigDecimal::compareTo)).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CustomerTransactionDTO> transactions() {
        List<CustomerTransactionDTO> data = new ArrayList<>();
        for (Order order : orderRepository.findAllByDeletedFalseOrderByCreatedAtDesc()) {
            Customer customer = order.getCustomer();
            if (customer == null) continue;
            BigDecimal net = netRevenue(order);
            String type = isReturnOrder(order) ? "REFUND" : "ORDER";
            data.add(new CustomerTransactionDTO(
                    order.getId(),
                    customer.getId(),
                    customer.getTen(),
                    customer.getEmail(),
                    order.getId(),
                    order.getOrderCode(),
                    order.getCreatedAt(),
                    nz(order.getFinalAmount()),
                    nz(order.getReturnedAmount()),
                    net,
                    order.getOrderStatus(),
                    order.getPaymentStatus(),
                    order.getChannel(),
                    type
            ));
        }
        return data;
    }

    @Transactional(readOnly = true)
    public CustomerDashboardDTO dashboard() {
        List<Customer> customers = customerRepository.findByStatus("ACTIVE");
        List<CustomerSpendingDTO> spending = spending();
        CustomerDashboardDTO dto = new CustomerDashboardDTO();

        dto.setTotalCustomers((long) customers.size());
        dto.setActiveCustomers((long) customers.size());
        dto.setVipCustomers(spending.stream().filter(c -> safePoint(c) > 0 || isVipRank(c.getRankName())).count());
        LocalDateTime firstDayOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        dto.setNewCustomersThisMonth(customers.stream()
                .filter(c -> c.getCreatedAt() != null && !c.getCreatedAt().isBefore(firstDayOfMonth))
                .count());
        dto.setCustomersWithOrders(spending.stream().filter(c -> c.getOrderCount() != null && c.getOrderCount() > 0).count());

        BigDecimal totalRevenue = spending.stream()
                .map(CustomerSpendingDTO::getTotalSpent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalCustomerRevenue(totalRevenue);
        long withOrders = Math.max(1L, dto.getCustomersWithOrders());
        dto.setAverageSpentPerCustomer(totalRevenue.divide(BigDecimal.valueOf(withOrders), 0, RoundingMode.HALF_UP));

        dto.setTopSpending(spending.stream().limit(5).collect(Collectors.toList()));
        dto.setTopVip(vipCustomers(null, null).stream().limit(5).collect(Collectors.toList()));
        dto.setLoyaltyCustomers(spending.stream()
                .filter(c -> c.getOrderCount() != null && c.getOrderCount() >= 2)
                .sorted(Comparator.comparing(CustomerSpendingDTO::getOrderCount, Comparator.nullsLast(Long::compareTo)).reversed())
                .limit(5)
                .collect(Collectors.toList()));
        dto.setNewCustomers(customers.stream()
                .sorted(Comparator.comparing(Customer::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                .limit(5)
                .map(this::baseSpending)
                .collect(Collectors.toList()));
        dto.setRecentTransactions(transactions().stream().limit(8).collect(Collectors.toList()));
        return dto;
    }

    private CustomerSpendingDTO baseSpending(Customer c) {
        return new CustomerSpendingDTO(
                c.getId(),
                c.getTen(),
                c.getEmail(),
                c.getPhone(),
                c.getLoaiKhach(),
                c.getDiemTichLuy() == null ? 0 : c.getDiemTichLuy(),
                BigDecimal.ZERO,
                0L,
                null
        );
    }

    private boolean isRevenueOrder(Order order) {
        if (order.getOrderStatus() == null) return false;
        boolean validStatus = order.getOrderStatus() == OrderStatus.COMPLETED
                || order.getOrderStatus() == OrderStatus.PARTIALLY_RETURNED
                || order.getOrderStatus() == OrderStatus.RETURNED;
        if (!validStatus) return false;
        return order.getPaymentStatus() == PaymentStatus.PAID
                || order.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED
                || order.getPaymentStatus() == PaymentStatus.REFUNDED;
    }

    private boolean isReturnOrder(Order order) {
        return order.getOrderStatus() == OrderStatus.PARTIALLY_RETURNED
                || order.getOrderStatus() == OrderStatus.RETURNED
                || nz(order.getReturnedAmount()).compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal netRevenue(Order order) {
        BigDecimal finalAmount = nz(order.getFinalAmount());
        BigDecimal returnedAmount = nz(order.getReturnedAmount());
        BigDecimal net = finalAmount.subtract(returnedAmount);
        return net.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : net;
    }

    private LocalDateTime revenueDate(Order order) {
        if (order.getCompletedAt() != null) return order.getCompletedAt();
        if (order.getDeliveredAt() != null) return order.getDeliveredAt();
        return order.getCreatedAt();
    }

    private int safePoint(CustomerSpendingDTO dto) {
        return dto.getPoint() == null ? 0 : dto.getPoint();
    }

    private boolean isVipRank(String rank) {
        if (rank == null) return false;
        String value = rank.trim().toUpperCase();
        return value.contains("VIP") || value.contains("GOLD") || value.contains("DIAMOND") || value.contains("PLATINUM");
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
