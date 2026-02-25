package com.sneakershop.backend.dto.promotion;

import com.sneakershop.backend.entity.pricing.ProductPrice;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.promotion.DiscountType;
import com.sneakershop.backend.entity.promotion.Promotion;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Data
public class PromotionDTO {

    private Long id;
    private String name;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private Integer priority;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean active;

    // dùng cho form
    private List<Long> variantIds;

    // dùng cho view detail
    private List<PromotionVariantDTO> variants;

    public static PromotionDTO fromEntity(Promotion p) {
        PromotionDTO dto = new PromotionDTO();

        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setCode(p.getCode());
        dto.setDiscountType(p.getDiscountType());
        dto.setDiscountValue(p.getDiscountValue());
        dto.setPriority(p.getPriority());
        dto.setStartTime(p.getStartTime());
        dto.setEndTime(p.getEndTime());
        dto.setActive(p.getActive());

        if (p.getVariants() == null || p.getVariants().isEmpty()) {
            dto.setVariantIds(List.of());
            dto.setVariants(List.of());
            return dto;
        }

        // danh sách id variant (cho form edit)
        dto.setVariantIds(
                p.getVariants().stream()
                        .map(ProductVariant::getId)
                        .toList()
        );

        // danh sách variant cho view
        dto.setVariants(
                p.getVariants().stream()
                        .map(v -> {
                            PromotionVariantDTO pv = new PromotionVariantDTO();

                            BigDecimal price = getCurrentPrice(v);
                            BigDecimal discountedPrice = calculateDiscountPrice(
                                    price,
                                    p.getDiscountType(),
                                    p.getDiscountValue()
                            );

                            pv.setVariantId(v.getId());
                            pv.setProductName(v.getProduct().getName());
                            pv.setColor(v.getColorway());
                            pv.setSize(
                                    v.getSize() == null ? null : Integer.valueOf(v.getSize())
                            );

                            pv.setPrice(price);
                            pv.setDiscountedPrice(discountedPrice);
                            pv.setStock(v.getStock());
                            pv.setImage(v.getProduct().getThumbnail());
                            return pv;
                        })
                        .toList()
        );

        return dto;
    }

    /** ✅ LẤY GIÁ HIỆN TẠI CỦA VARIANT */
    private static BigDecimal getCurrentPrice(ProductVariant v) {
        if (v.getPrices() == null || v.getPrices().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return v.getPrices().stream()
                .filter(p -> p.getEndDate() == null) // giá đang active
                .max(Comparator.comparing(ProductPrice::getStartDate))
                .map(ProductPrice::getPrice)
                .orElse(BigDecimal.ZERO);
    }

    /** ✅ TÍNH GIÁ SAU GIẢM */
    private static BigDecimal calculateDiscountPrice(
            BigDecimal price,
            DiscountType type,
            BigDecimal value
    ) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (type == DiscountType.PERCENT) {
            return price.subtract(
                    price.multiply(value).divide(BigDecimal.valueOf(100))
            );
        }

        if (type == DiscountType.AMOUNT) {
            return price.subtract(value).max(BigDecimal.ZERO);
        }

        return price;
    }
}
