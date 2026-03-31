package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.entity.promotion.PromotionDetail;
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

    public BigDecimal calculateFinalPrice(Long variantId, String loaiKhach) {

        BigDecimal originalPrice = productPriceRepository
                .findActivePrice(variantId)
                .map(ProductPrice::getPrice)
                .orElse(BigDecimal.ZERO);

        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal groupDiscountAmount = variantPriceGroupRepository
                .findByVariant_IdAndLoaiKhach(variantId, loaiKhach)
                .map(VariantPriceGroup::getPrice)
                .orElse(BigDecimal.ZERO);

        BigDecimal priceAfterGroup = originalPrice.subtract(groupDiscountAmount).max(BigDecimal.ZERO);

        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findActivePromotions(variantId, now);

        if (promotions == null || promotions.isEmpty()) {
            return priceAfterGroup.setScale(0, RoundingMode.HALF_UP);
        }

        // Tìm KM tốt nhất, truyền thêm variantId vào hàm tính toán
        Promotion bestPromotion = promotions.stream()
                .filter(p -> {
                    String group = p.getCustomerGroup();
                    return group == null
                            || "ALL".equalsIgnoreCase(group)
                            || (loaiKhach != null && loaiKhach.equalsIgnoreCase(group));
                })
                .max(
                        Comparator.comparing((Promotion p) -> p.getPriority() == null ? 0 : p.getPriority())
                                .thenComparing(p -> calculateDiscountAmount(priceAfterGroup, p, variantId))
                )
                .orElse(null);

        if (bestPromotion == null) {
            return priceAfterGroup.setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal promoDiscountAmount = calculateDiscountAmount(priceAfterGroup, bestPromotion, variantId);
        BigDecimal finalPrice = priceAfterGroup.subtract(promoDiscountAmount);

        return finalPrice.max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDiscountAmount(BigDecimal basePrice, Promotion promotion, Long variantId) {
        if (promotion == null) {
            return BigDecimal.ZERO;
        }

        // Lấy thông số KM riêng của sản phẩm này
        PromotionDetail detail = promotion.getPromotionDetails().stream()
                .filter(pd -> pd.getVariant().getId().equals(variantId))
                .findFirst()
                .orElse(null);

        if (detail == null || detail.getDiscountType() == null || detail.getDiscountValue() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if ("PERCENT".equals(detail.getDiscountType().name())) {
            discount = basePrice
                    .multiply(detail.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            discount = detail.getDiscountValue();
        }
        return discount.min(basePrice);
    }

    public Map<Long, BigDecimal> calculateFinalPriceBoard(String loaiKhach) {
        Map<Long, BigDecimal> result = new HashMap<>();
        List<ProductPrice> prices = productPriceRepository.findAll();
        for (ProductPrice price : prices) {
            Long variantId = price.getVariant().getId();
            BigDecimal finalPrice = calculateFinalPrice(variantId, loaiKhach);
            result.put(variantId, finalPrice);
        }
        return result;
    }
}