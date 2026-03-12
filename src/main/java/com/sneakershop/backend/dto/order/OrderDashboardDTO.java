package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDashboardDTO {
    private Long totalOrders;
    private Long newOrders;
    private Long processingOrders;
    private Long shippingOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long returnedOrders;

    private BigDecimal revenueToday;
    private BigDecimal revenueThisMonth;
    private BigDecimal totalRevenue;

    private Long totalReturnedQuantity;

    private List<BestSellingProductDTO> topProducts;
    private List<ReturnedProductStatisticDTO> topReturnedProducts;
    private List<CustomerRevenueDTO> revenueByCustomer;
    private List<DailyRevenueDTO> revenueDaily;
    private List<MonthlyRevenueDTO> revenueMonthly;
}