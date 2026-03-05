package com.sneakershop.backend.entity.order.enums;

public enum ReturnStatus {
    NONE,               // Chưa trả
    PARTIALLY_RETURNED, // Trả 1 phần
    COMPLETED,
    RETURNED            // Trả hết
}