package com.sneakershop.backend.dto.product;

import lombok.Data;
import java.util.List;

@Data
public class UpdateProductTagsRequest {

    private List<Long> tagIds;

}