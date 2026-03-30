package com.sneakershop.backend.dto.promotion;

import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.promotion.DiscountType;
import com.sneakershop.backend.entity.promotion.Promotion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    private List<Long> variantIds;
    private List<PromotionVariantDTO> variants;

    // 🔥 URL Server để Frontend không phải tự ghép
    private static final String IMAGE_BASE_URL = "http://localhost:8080/";

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

        if (p.getVariants() != null) {
            dto.setVariantIds(p.getVariants().stream().map(ProductVariant::getId).toList());
            dto.setVariants(p.getVariants().stream().map(v -> {
                PromotionVariantDTO pv = new PromotionVariantDTO();
                pv.setVariantId(v.getId());
                pv.setProductName(v.getProduct().getName());

                // 🔥 ĐÃ FIX: Lấy Tên Màu sắc
                pv.setColor(v.getColor() != null ? v.getColor().getName() : null);

                // 🔥 ĐÃ FIX: Ép kiểu an toàn cho Size
                Integer sizeVal = null;
                if (v.getSize() != null && v.getSize().getName() != null) {
                    try {
                        sizeVal = Integer.valueOf(v.getSize().getName().trim());
                    } catch (NumberFormatException ignored) {}
                }
                pv.setSize(sizeVal);

                pv.setStock(v.getStock());

                // 🔥 FIX ẢNH: Gắn thumbnail và tự ghép domain
                String thumbPath = v.getProduct().getThumbnail();
                if (thumbPath != null && !thumbPath.startsWith("http")) {
                    pv.setThumbnail(IMAGE_BASE_URL + thumbPath);
                } else {
                    pv.setThumbnail(thumbPath);
                }

                // 🔥 FIX GIÁ: Ưu tiên lấy giá bán, nếu ko có thì lấy giá gốc
                BigDecimal currentPrice = v.getSalePrice() != null && v.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                        ? v.getSalePrice() : (v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO);

                pv.setPrice(currentPrice);
                pv.setDiscountedPrice(calculateDiscountPrice(currentPrice, p.getDiscountType(), p.getDiscountValue()));

                return pv;
            }).toList());
        }
        return dto;
    }

    private static BigDecimal calculateDiscountPrice(BigDecimal price, DiscountType type, BigDecimal value) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        if (type == DiscountType.PERCENT) {
            return price.subtract(price.multiply(value).divide(BigDecimal.valueOf(100)));
        }
        return price.subtract(value).max(BigDecimal.ZERO);
    }
}