package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.pricing.VariantPriceGroup;
import com.sneakershop.backend.entity.promotion.Promotion;
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

    /**
     * Tính giá cuối cùng: (Giá gốc - Giảm nhóm) - Khuyến mãi
     */
    public BigDecimal calculateFinalPrice(Long variantId, String loaiKhach) {

        // 1️⃣ Lấy GIÁ GỐC THỰC TẾ (Ví dụ: 1.000.000)
        BigDecimal originalPrice = productPriceRepository
                .findActivePrice(variantId)
                .map(ProductPrice::getPrice)
                .orElse(BigDecimal.ZERO);

        if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 2️⃣ Lấy SỐ TIỀN GIẢM của nhóm khách hàng (Ví dụ: VIP giảm 300.000)
        BigDecimal groupDiscountAmount = variantPriceGroupRepository
                .findByVariant_IdAndLoaiKhach(variantId, loaiKhach)
                .map(VariantPriceGroup::getPrice)
                .orElse(BigDecimal.ZERO);

        // 3️⃣ Tính giá sau khi đã giảm nhóm (1.000.000 - 300.000 = 700.000)
        // Đây chính là basePrice mới để tính Khuyến mãi
        BigDecimal priceAfterGroup = originalPrice.subtract(groupDiscountAmount).max(BigDecimal.ZERO);

        // 4️⃣ Lấy promotion active
        LocalDateTime now = LocalDateTime.now();
        List<Promotion> promotions = promotionRepository.findActivePromotions(variantId, now);

        if (promotions == null || promotions.isEmpty()) {
            return priceAfterGroup.setScale(0, RoundingMode.HALF_UP);
        }

        // 5️⃣ Tìm KM tốt nhất áp dụng trên giá đã giảm nhóm (700.000)
        Promotion bestPromotion = promotions.stream()
                .filter(p -> {
                    String group = p.getCustomerGroup();
                    return group == null
                            || "ALL".equalsIgnoreCase(group)
                            || (loaiKhach != null && loaiKhach.equalsIgnoreCase(group));
                })
                .max(
                        Comparator.comparing((Promotion p) -> p.getPriority() == null ? 0 : p.getPriority())
                                .thenComparing(p -> calculateDiscountAmount(priceAfterGroup, p))
                )
                .orElse(null);

        if (bestPromotion == null) {
            return priceAfterGroup.setScale(0, RoundingMode.HALF_UP);
        }

        // 6️⃣ Tính tiền giảm KM (Ví dụ: 700.000 * 50% = 350.000)
        BigDecimal promoDiscountAmount = calculateDiscountAmount(priceAfterGroup, bestPromotion);

        // 7️⃣ Giá cuối cùng (700.000 - 350.000 = 350.000)
        BigDecimal finalPrice = priceAfterGroup.subtract(promoDiscountAmount);

        return finalPrice.max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Tính tiền giảm - CHUYỂN THÀNH PUBLIC để Service khác gọi được
     */
    public BigDecimal calculateDiscountAmount(BigDecimal basePrice, Promotion promotion) {
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