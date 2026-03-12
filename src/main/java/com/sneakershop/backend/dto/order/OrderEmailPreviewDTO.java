package com.sneakershop.backend.dto.order;

import lombok.Data;

@Data
public class OrderEmailPreviewDTO {
    private Long orderId;
    private String orderCode;
    private String toEmail;
    private String subject;
    private String content;
    private Boolean markedSent;
}