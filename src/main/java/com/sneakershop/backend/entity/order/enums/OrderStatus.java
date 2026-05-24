package com.sneakershop.backend.entity.order.enums;

public enum OrderStatus {
    NEW,        // Mới
    PROCESSING, // Đang xử lý
    SHIPPING,   // Đang giao
    COMPLETED,  // Hoàn tất giao hàng
    PARTIALLY_RETURNED, // Đã hoàn trả một phần
    RETURNED,    // Đã hoàn trả toàn bộ
    CANCELLED   // Hủy
}