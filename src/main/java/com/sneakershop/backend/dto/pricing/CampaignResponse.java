package com.sneakershop.backend.dto.pricing;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CampaignResponse {

    private Long id;

    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Boolean active;

    private List<CampaignItemDTO> items;

}