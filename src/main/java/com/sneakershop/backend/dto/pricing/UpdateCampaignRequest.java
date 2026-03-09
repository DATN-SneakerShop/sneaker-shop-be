package com.sneakershop.backend.dto.pricing;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateCampaignRequest {

    // Tên chiến dịch
    private String name;

    // Thời gian bắt đầu
    private LocalDateTime startTime;

    // Thời gian kết thúc
    private LocalDateTime endTime;

    // Trạng thái
    private Boolean active;
    private List<CampaignItemDTO> items;

}

