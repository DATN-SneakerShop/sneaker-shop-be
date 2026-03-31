package com.sneakershop.backend.dto.promotion;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BasePromotionRequest {
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean active;
    private Integer priority;

    private List<PromotionDetailRequest> details;
}