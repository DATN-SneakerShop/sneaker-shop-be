package com.sneakershop.backend.service.promotion;

import com.sneakershop.backend.dto.promotion.PromotionDTO;
import com.sneakershop.backend.repository.promotion.PromotionRepository;
import com.sneakershop.backend.repository.voucher.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional // 🔥 BẮT BUỘC PHẢI CÓ ĐỂ TRÁNH LỖI MẤT KẾT NỐI DATABASE (Lazy Load)
public class PromotionReportService {

    private final VoucherRepository voucherRepository;
    private final PromotionRepository promotionRepository;

    public Map<String, Object> getActivePromotionReport() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Lấy danh sách Voucher đang chạy
        var activeVouchers = voucherRepository.findActiveVouchersForReport(now);

        // 2. Lấy danh sách Đợt giảm giá đang chạy và ÉP SANG DTO
        var activePromotions = promotionRepository.findActivePromotionsForReport(now)
                .stream()
                .map(PromotionDTO::fromEntity) // 🔥 Tránh ném Entity thô ra ngoài gây lỗi
                .collect(Collectors.toList());

        // 3. Đóng gói vào Map để trả về một báo cáo tổng hợp
        Map<String, Object> report = new HashMap<>();
        report.put("totalActive", activeVouchers.size() + activePromotions.size());
        report.put("vouchers", activeVouchers);
        report.put("promotions", activePromotions);
        report.put("reportDate", now);

        return report;
    }
}