package com.sneakershop.backend.dto.product;

import java.util.List;
import lombok.Data;
@Data
public class BatchUpdateStatusRequest {
    private List<Long> ids;

    private String status;
}
