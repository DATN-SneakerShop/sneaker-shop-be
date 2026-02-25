package com.sneakershop.backend.service.pricing;

import com.sneakershop.backend.dto.pricing.PriceBoardDTO;

import com.sneakershop.backend.dto.pricing.ProductCardDTO;
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
public class ProductPricingService {

    private final ProductPriceRepository priceRepository;
    private final PromotionRepository promotionRepository;


    public List<ProductCardDTO> getProductCards() {

        List<PriceBoardDTO> prices = priceRepository.getCurrentPriceBoard();

        return prices.stream().map(price -> {

            BigDecimal originalPrice = price.getPrice();

            // 🔥 LẤY TẤT CẢ KM
            List<Promotion> promotions =
                    promotionRepository.findAllActivePromotionsByVariant(
                            price.getVariantId(),
                            LocalDateTime.now()
                    );

            BigDecimal bestFinal = originalPrice;
            BigDecimal bestDiscount = BigDecimal.ZERO;
            String bestName = null;
            Integer discountPercent = null;
            boolean onSale = false;

            if (!promotions.isEmpty()) {

                onSale = true;

                for (Promotion promotion : promotions) {

                    BigDecimal finalPrice = originalPrice;

                    // ===== COMBO =====
                    if (promotion.getDiscountType() == DiscountType.BUY_2_GET_1) {

                        // giả sử hiển thị theo 1 sản phẩm → giá trung bình
                        BigDecimal comboPrice = originalPrice
                                .multiply(BigDecimal.valueOf(2))
                                .divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);

                        finalPrice = comboPrice;
                    }

                    // ===== PERCENT =====
                    else if (promotion.getDiscountType() == DiscountType.PERCENT) {

                        BigDecimal discount = originalPrice
                                .multiply(promotion.getDiscountValue())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                        finalPrice = originalPrice.subtract(discount);
                    }

                    // ===== AMOUNT =====
                    else if (promotion.getDiscountType() == DiscountType.AMOUNT) {

                        finalPrice = originalPrice.subtract(promotion.getDiscountValue());

                        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                            finalPrice = BigDecimal.ZERO;
                        }
                    }

                    // 🔥 chọn giá thấp nhất
                    if (finalPrice.compareTo(bestFinal) < 0) {

                        bestFinal = finalPrice;
                        bestDiscount = originalPrice.subtract(finalPrice);
                        bestName = promotion.getName();

                        // tính % hiển thị
                        discountPercent = originalPrice.compareTo(BigDecimal.ZERO) > 0
                                ? bestDiscount.multiply(BigDecimal.valueOf(100))
                                .divide(originalPrice, 0, RoundingMode.HALF_UP)
                                .intValue()
                                : 0;
                    }
                }
            }

            return new ProductCardDTO(
                    price.getVariantId(),
                    price.getProductName(),
                    price.getSku(),
                    price.getColorway(),
                    price.getSize(),
                    originalPrice,
                    bestFinal,
                    bestDiscount,
                    bestName,
                    discountPercent,
                    onSale
            );

        }).toList();
    }
}