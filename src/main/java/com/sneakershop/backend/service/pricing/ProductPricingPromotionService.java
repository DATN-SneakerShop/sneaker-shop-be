package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.dto.pricing.PriceResultDTO;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.promotion.DiscountType;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.promotion.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductPricingPromotionService {

    private final ProductPriceRepository priceRepository;
    private final PromotionRepository promotionRepository;


    public PriceResultDTO calculateFinalPrice(Long variantId, int quantity) {

        // ===== 1. LẤY GIÁ GỐC =====
        ProductPrice price = priceRepository.findActivePrice(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giá"));

        BigDecimal unitPrice = price.getPrice();
        BigDecimal originalTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // ===== 2. LẤY DANH SÁCH KM ACTIVE =====
        List<Promotion> promotions =
                promotionRepository.findAllActivePromotionsByVariant(
                        variantId,
                        LocalDateTime.now()
                );

        if (promotions.isEmpty()) {
            return new PriceResultDTO(
                    originalTotal,
                    BigDecimal.ZERO,
                    originalTotal,
                    null,
                    false
            );
        }

        // ===== 3. TÌM KM TỐT NHẤT =====
        BigDecimal bestFinal = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        String bestName = null;
        Integer bestPriority = 0;
        Long bestId = null;

        for (Promotion promotion : promotions) {

            BigDecimal finalTotal = calculatePromotionTotal(
                    promotion,
                    unitPrice,
                    quantity,
                    originalTotal
            );

            Integer currentPriority =
                    promotion.getPriority() != null ? promotion.getPriority() : 0;

            Long currentId = promotion.getId();

            boolean isBetter = false;

            if (bestFinal == null) {
                isBetter = true;
            }
            // 1️⃣ Giá thấp hơn
            else if (finalTotal.compareTo(bestFinal) < 0) {
                isBetter = true;
            }
            // 2️⃣ Giá bằng → priority cao hơn
            else if (finalTotal.compareTo(bestFinal) == 0
                    && currentPriority > bestPriority) {
                isBetter = true;
            }
            // 3️⃣ Giá bằng + priority bằng → ID cao hơn
            else if (finalTotal.compareTo(bestFinal) == 0
                    && currentPriority.equals(bestPriority)
                    && currentId > bestId) {
                isBetter = true;
            }

            if (isBetter) {
                bestFinal = finalTotal;
                bestDiscount = originalTotal.subtract(finalTotal);
                bestName = promotion.getName();
                bestPriority = currentPriority;
                bestId = currentId;
            }
        }

        return new PriceResultDTO(
                originalTotal,
                bestDiscount,
                bestFinal,
                bestName,
                true
        );
    }

    // ================= CORE CALCULATION =================

    private BigDecimal calculatePromotionTotal(
            Promotion promotion,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal originalTotal
    ) {

        switch (promotion.getDiscountType()) {

            case PERCENT:
                BigDecimal percent = promotion.getDiscountValue();

                if (percent.compareTo(BigDecimal.valueOf(100)) > 0) {
                    percent = BigDecimal.valueOf(100);
                }

                BigDecimal discount = originalTotal
                        .multiply(percent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                return originalTotal.subtract(discount);

            case AMOUNT:
                BigDecimal afterAmount =
                        originalTotal.subtract(promotion.getDiscountValue());

                return afterAmount.max(BigDecimal.ZERO);

            case BUY_2_GET_1:
                int buy = 2;
                int get = 1;

                int groupSize = buy + get;
                int free = (quantity / groupSize) * get;
                int pay = quantity - free;

                return unitPrice.multiply(BigDecimal.valueOf(pay));

            default:
                return originalTotal;
        }
    }
}