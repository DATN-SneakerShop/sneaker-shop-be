package com.sneakershop.backend.dto.promotion;

import com.sneakershop.backend.entity.promotion.DiscountType;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BasePromotionRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String code;

    @NotNull
    private DiscountType discountType; // 🔥 BẮT BUỘC

    @NotNull
    private BigDecimal discountValue;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    private Boolean active = true;

    private Integer priority;

    private List<Long> variantIds;
}