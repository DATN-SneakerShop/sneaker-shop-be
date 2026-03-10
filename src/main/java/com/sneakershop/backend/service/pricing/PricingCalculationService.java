package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.dto.pricing.PriceGroupResponse;
import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.repository.pricing.PriceCampaignItemRepository;
import com.sneakershop.backend.repository.pricing.ProductPriceRepository;
import com.sneakershop.backend.repository.pricing.VariantPriceGroupRepository;
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

    private final VariantPriceGroupRepository variantPriceGroupRepository;
    private final ProductPriceRepository productPriceRepository;
    private final PromotionRepository promotionRepository;
    private final PriceCampaignItemRepository campaignItemRepository;

    /**
     * Tính giá cuối cùng
     */
    public BigDecimal calculateFinalPrice(Long variantId, String loaiKhach) {

        // 1️⃣ Lấy giá group nếu có
        BigDecimal basePrice = variantPriceGroupRepository
                .findByVariant_IdAndLoaiKhach(variantId, loaiKhach)
                .map(VariantPriceGroup::getPrice)
                .orElseGet(() ->
                        productPriceRepository
                                .findActivePrice(variantId)
                                .map(ProductPrice::getPrice)
                                .orElse(BigDecimal.ZERO)
                );

        if (basePrice == null) {
            return BigDecimal.ZERO;
        }

        // 2️⃣ Lấy promotion active
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findActivePromotions(variantId, now);

        if (promotions == null || promotions.isEmpty()) {
            return basePrice;
        }

        // 3️⃣ Lọc promotion theo nhóm khách và tìm KM tốt nhất
        Promotion bestPromotion = promotions.stream()
                .filter(p -> {
                    String group = p.getCustomerGroup();
                    return group == null
                            || "ALL".equalsIgnoreCase(group)
                            || (loaiKhach != null && loaiKhach.equalsIgnoreCase(group));
                })
                .max(
                        Comparator.comparing((Promotion p) -> p.getPriority() == null ? 0 : p.getPriority())
                                .thenComparing(p -> calculateDiscountAmount(basePrice, p))
                )
                .orElse(null);

        if (bestPromotion == null) {
            return basePrice;
        }

        // 4️⃣ Tính tiền giảm
        BigDecimal discountAmount =
                calculateDiscountAmount(basePrice, bestPromotion);

        // 5️⃣ Giá cuối
        BigDecimal finalPrice = basePrice.subtract(discountAmount);

        return finalPrice.max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính tiền giảm
     */
    private BigDecimal calculateDiscountAmount(BigDecimal basePrice, Promotion promotion) {

        if (promotion == null || promotion.getDiscountType() == null || promotion.getDiscountValue() == null) {
            return BigDecimal.ZERO;
        }

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

    /**
     * Bảng giá final cho toàn bộ variant
     */
    public Map<Long, BigDecimal> calculateFinalPriceBoard(String loaiKhach) {

        Map<Long, BigDecimal> result = new HashMap<>();

        List<ProductPrice> prices = productPriceRepository.findAll();

        for (ProductPrice price : prices) {

            Long variantId = price.getVariant().getId();

            BigDecimal finalPrice =
                    calculateFinalPrice(variantId, loaiKhach);

            result.put(variantId, finalPrice);

        }

        return result;
    }

    public BigDecimal getCampaignPrice(Long variantId) {

        LocalDateTime now = LocalDateTime.now();

        return campaignItemRepository.findAll()
                .stream()
                .filter(i ->
                        i.getVariant().getId().equals(variantId)
                                && i.getCampaign().getActive()
                                && now.isAfter(i.getCampaign().getStartTime())
                                && now.isBefore(i.getCampaign().getEndTime())
                )
                .map(i -> i.getPrice())
                .min(BigDecimal::compareTo)
                .orElse(null);
    }
}