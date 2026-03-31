package com.sneakershop.backend.dto.promotion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.promotion.DiscountType;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.entity.promotion.PromotionDetail;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    private static final String IMAGE_BASE_URL = "http://localhost:8080/";

    public static PromotionDTO fromEntity(Promotion p) {
        PromotionDTO dto = new PromotionDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setCode(p.getCode());
        dto.setPriority(p.getPriority());
        dto.setStartTime(p.getStartTime());
        dto.setEndTime(p.getEndTime());
        dto.setActive(p.getActive());

        if (p.getPromotionDetails() != null && !p.getPromotionDetails().isEmpty()) {

            // 🔥 ĐÃ FIX: Lấy thông tin giảm giá từ detail đầu tiên (đại diện cho đợt khuyến mãi)
            PromotionDetail firstDetail = p.getPromotionDetails().get(0);
            dto.setDiscountType(firstDetail.getDiscountType());
            dto.setDiscountValue(firstDetail.getDiscountValue());

            dto.setVariantIds(p.getPromotionDetails().stream()
                    .map(pd -> pd.getVariant().getId())
                    .toList());

            dto.setVariants(p.getPromotionDetails().stream().map(pd -> {
                ProductVariant v = pd.getVariant();
                PromotionVariantDTO pv = new PromotionVariantDTO();
                pv.setVariantId(v.getId());
                pv.setProductName(v.getProduct().getName());

                // Lấy Tên Màu sắc
                pv.setColor(v.getColor() != null ? v.getColor().getName() : null);

                // Ép kiểu an toàn cho Size
                Integer sizeVal = null;
                if (v.getSize() != null && v.getSize().getName() != null) {
                    try {
                        sizeVal = Integer.valueOf(v.getSize().getName().trim());
                    } catch (NumberFormatException ignored) {}
                }
                pv.setSize(sizeVal);

                pv.setStock(v.getStock());
                pv.setDiscountType(pd.getDiscountType());
                pv.setDiscountValue(pd.getDiscountValue());

                String variantImg = v.getImageUrl();
                String productThumb = v.getProduct().getThumbnail();
                String finalPath = (variantImg != null && !variantImg.isEmpty()) ? variantImg : productThumb;

                if (finalPath != null && !finalPath.startsWith("http")) {
                    pv.setThumbnail(IMAGE_BASE_URL + finalPath);
                } else {
                    pv.setThumbnail(finalPath);
                }
                BigDecimal currentPrice = v.getSalePrice() != null && v.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                        ? v.getSalePrice() : (v.getPrice() != null ? v.getPrice() : BigDecimal.ZERO);

                pv.setPrice(currentPrice);

                pv.setDiscountedPrice(calculateDiscountPrice(currentPrice, pd.getDiscountType(), pd.getDiscountValue()));

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