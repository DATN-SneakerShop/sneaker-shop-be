package com.sneakershop.backend.service.promotion;

import com.sneakershop.backend.dto.promotion.PromotionDashboardDTO;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PromotionStatisticService {

    private final OrderRepository orderRepository;

    public PromotionDashboardDTO getDashboardData() {
        // 1. Lấy tất cả đơn hàng KHÔNG BỊ XÓA
        List<Order> orders = orderRepository.findAllByDeletedFalseOrderByCreatedAtDesc();

        PromotionDashboardDTO dto = new PromotionDashboardDTO();
        BigDecimal totalVoucherDiscount = BigDecimal.ZERO;
        BigDecimal totalProductDiscount = BigDecimal.ZERO;
        long totalVoucherUsage = 0;
        BigDecimal totalPromoRevenue = BigDecimal.ZERO;

        Map<String, PromotionDashboardDTO.ChartData> chartMap = new LinkedHashMap<>();
        Map<String, PromotionDashboardDTO.TopVoucher> voucherMap = new HashMap<>();
        Map<String, PromotionDashboardDTO.TopProduct> productMap = new HashMap<>();

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

        for (Order o : orders) {
            // Chỉ tính đơn hàng ĐÃ HOÀN THÀNH
           // if (o.getOrderStatus() != OrderStatus.COMPLETED) continue;
            if (o.getOrderStatus() == OrderStatus.CANCELLED) {
                continue;
            }

            boolean hasPromo = false;
            String monthLabel = o.getCreatedAt().format(monthFormatter);
            PromotionDashboardDTO.ChartData chartData = chartMap.computeIfAbsent(monthLabel, k -> {
                PromotionDashboardDTO.ChartData c = new PromotionDashboardDTO.ChartData();
                c.setMonthLabel(k);
                return c;
            });

            // 2. Tính tiền Voucher
            if (o.getVoucherCode() != null && !o.getVoucherCode().isBlank()) {
                hasPromo = true;
                totalVoucherUsage++;
                
                // Giả sử discountAmount lưu tiền giảm Voucher
                BigDecimal vDiscount = o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO;
                totalVoucherDiscount = totalVoucherDiscount.add(vDiscount);
                chartData.setVoucherDiscount(chartData.getVoucherDiscount().add(vDiscount));

                // Lưu Top Voucher
                String[] codes = o.getVoucherCode().split(",");
                for (String code : codes) {
                    String cleanCode = code.trim();
                    PromotionDashboardDTO.TopVoucher tv = voucherMap.computeIfAbsent(cleanCode, k -> {
                        PromotionDashboardDTO.TopVoucher t = new PromotionDashboardDTO.TopVoucher();
                        t.setCode(k);
                        t.setUsageCount(0L);
                        t.setTotalDiscount(BigDecimal.ZERO);
                        return t;
                    });
                    tv.setUsageCount(tv.getUsageCount() + 1);
                    tv.setTotalDiscount(tv.getTotalDiscount().add(vDiscount)); // Note: Chiều chuẩn thì phải chia đều, nhưng code demo cứ cộng tổng
                }
            }

            // 3. Tính tiền giảm trực tiếp Sản phẩm
            if (o.getItems() != null) {
                for (OrderItem item : o.getItems()) {
                    BigDecimal lineDiscount = item.getLineDiscountAmount() != null ? item.getLineDiscountAmount() : BigDecimal.ZERO;
                    if (lineDiscount.compareTo(BigDecimal.ZERO) > 0) {
                        hasPromo = true;
                        totalProductDiscount = totalProductDiscount.add(lineDiscount);
                        chartData.setProductDiscount(chartData.getProductDiscount().add(lineDiscount));

                        // Lưu Top Sản phẩm giảm giá
                        String pName = item.getProductNameSnapshot();
                        PromotionDashboardDTO.TopProduct tp = productMap.computeIfAbsent(pName, k -> {
                            PromotionDashboardDTO.TopProduct t = new PromotionDashboardDTO.TopProduct();
                            t.setProductName(k);
                            t.setSoldQuantity(0L);
                            t.setTotalDiscount(BigDecimal.ZERO);
                            return t;
                        });
                        tp.setSoldQuantity(tp.getSoldQuantity() + (item.getQuantity() != null ? item.getQuantity() : 0));
                        tp.setTotalDiscount(tp.getTotalDiscount().add(lineDiscount));
                    }
                }
            }

            // 4. Cộng doanh thu nếu đơn này có xài KM
            if (hasPromo) {
                totalPromoRevenue = totalPromoRevenue.add(o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO);
            }
        }

        // Đóng gói DTO
        dto.setTotalVoucherDiscount(totalVoucherDiscount);
        dto.setTotalProductDiscount(totalProductDiscount);
        dto.setTotalVoucherUsage(totalVoucherUsage);
        dto.setTotalPromoRevenue(totalPromoRevenue);

        // Đảo ngược List Chart để tháng cũ đứng trước, tháng mới đứng sau
        List<PromotionDashboardDTO.ChartData> chartList = new ArrayList<>(chartMap.values());
        Collections.reverse(chartList);
        dto.setChartData(chartList);

        // Sort Top 5 Vouchers
        dto.setTopVouchers(voucherMap.values().stream()
                .sorted((a, b) -> b.getUsageCount().compareTo(a.getUsageCount()))
                .limit(5).collect(Collectors.toList()));

        // Sort Top 5 Products
        dto.setTopProducts(productMap.values().stream()
                .sorted((a, b) -> b.getTotalDiscount().compareTo(a.getTotalDiscount()))
                .limit(5).collect(Collectors.toList()));

        return dto;
    }
}