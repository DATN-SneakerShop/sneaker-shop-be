package com.sneakershop.backend.dto.promotion;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PromotionDashboardDTO {
    // Thẻ tổng quan
    private BigDecimal totalVoucherDiscount; // Tổng tiền giảm bằng Voucher
    private BigDecimal totalProductDiscount; // Tổng tiền giảm trực tiếp trên SP
    private Long totalVoucherUsage;          // Tổng lượt dùng Voucher
    private BigDecimal totalPromoRevenue;    // Doanh thu từ các đơn có khuyến mãi

    // Biểu đồ & Bảng xếp hạng
    private List<ChartData> chartData;
    private List<TopVoucher> topVouchers;
    private List<TopProduct> topProducts;

    // --- CÁC CLASS CON BÊN TRONG CHO GỌN ---
    @Data
    public static class ChartData {
        private String monthLabel;
        private BigDecimal voucherDiscount = BigDecimal.ZERO;
        private BigDecimal productDiscount = BigDecimal.ZERO;
    }

    @Data
    public static class TopVoucher {
        private String code;
        private Long usageCount;
        private BigDecimal totalDiscount;
    }

    @Data
    public static class TopProduct {
        private String productName;
        private Long soldQuantity;
        private BigDecimal totalDiscount;
    }
}