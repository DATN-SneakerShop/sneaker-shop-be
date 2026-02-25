package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.dto.pricing.PriceGroupResponse;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.repository.promotion.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PricingCalculationService {

    private final VariantPriceGroupService variantPriceGroupService;
    private final PromotionRepository promotionRepository;

    public BigDecimal calculateFinalPrice(Long variantId, String loaiKhach) {

        // 1️⃣ Lấy giá theo nhóm khách (hoặc default)
        BigDecimal basePrice =
                variantPriceGroupService.getPriceByCustomerType(variantId, loaiKhach);

        if (basePrice == null) {
            return BigDecimal.ZERO;
        }

        // 2️⃣ Lấy promotion đang active theo thời gian + variant
        LocalDateTime now = LocalDateTime.now();

        List<Promotion> promotions =
                promotionRepository.findActivePromotions(
                        variantId,
                        now
                );

        if (promotions.isEmpty()) {
            return basePrice;
        }

        // 3️⃣ Lọc promotion theo nhóm khách
        Promotion bestPromotion = promotions.stream()
                .filter(p -> {
                    String group = p.getCustomerGroup();
                    return group == null
                            || "ALL".equalsIgnoreCase(group)
                            || loaiKhach.equalsIgnoreCase(group);
                })
                .sorted(
                        Comparator
                                .comparing(
                                        (Promotion p) ->
                                                p.getPriority() == null ? 0 : p.getPriority()
                                )
                                .reversed()
                                .thenComparing(
                                        p -> calculateDiscountAmount(basePrice, p),
                                        Comparator.reverseOrder()
                                )
                )
                .findFirst()
                .orElse(null);

        if (bestPromotion == null) {
            return basePrice;
        }

        // 4️⃣ Tính tiền giảm
        BigDecimal discountAmount = calculateDiscountAmount(basePrice, bestPromotion);

        // 5️⃣ Giá cuối cùng (không cho âm)
        BigDecimal finalPrice = basePrice.subtract(discountAmount);

        return finalPrice.max(BigDecimal.ZERO)
                .setScale(0, RoundingMode.HALF_UP);
    }
    /**
     * Tính số tiền được giảm (không phải %)
     */
    private BigDecimal calculateDiscountAmount(BigDecimal basePrice, Promotion promotion) {

        BigDecimal discount;

        if ("PERCENT".equals(promotion.getDiscountType().name())) {

            discount = basePrice
                    .multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        } else {
            discount = promotion.getDiscountValue();
        }
        return discount.min(basePrice);
    }
    public Map<Long, BigDecimal> calculateFinalPriceBoard(String loaiKhach) {

        Map<Long, BigDecimal> result = new HashMap<>();

        List<PriceGroupResponse> variants =
                variantPriceGroupService.getAll();

        for (PriceGroupResponse variant : variants) {

            BigDecimal finalPrice =
                    calculateFinalPrice(variant.getVariantId(), loaiKhach);

            result.put(variant.getVariantId(), finalPrice);
        }

        return result;
    }

}