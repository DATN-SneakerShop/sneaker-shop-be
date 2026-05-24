package com.sneakershop.backend.dto.customer;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerDashboardDTO {
    private Long totalCustomers = 0L;
    private Long activeCustomers = 0L;
    private Long vipCustomers = 0L;
    private Long newCustomersThisMonth = 0L;
    private Long customersWithOrders = 0L;
    private BigDecimal totalCustomerRevenue = BigDecimal.ZERO;
    private BigDecimal averageSpentPerCustomer = BigDecimal.ZERO;
    private List<CustomerSpendingDTO> topVip = new ArrayList<>();
    private List<CustomerSpendingDTO> topSpending = new ArrayList<>();
    private List<CustomerSpendingDTO> loyaltyCustomers = new ArrayList<>();
    private List<CustomerSpendingDTO> newCustomers = new ArrayList<>();
    private List<CustomerTransactionDTO> recentTransactions = new ArrayList<>();
}
