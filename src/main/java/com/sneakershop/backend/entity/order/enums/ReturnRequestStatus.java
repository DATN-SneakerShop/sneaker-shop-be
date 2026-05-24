package com.sneakershop.backend.entity.order.enums;

public enum ReturnRequestStatus {
    REQUESTED,  // Đã tạo đơn hoàn trả, chờ admin nhận hàng
    RECEIVED,   // Admin đã xác nhận nhận hàng hoàn
    ACCEPTED,   // Admin đã duyệt hàng hoàn và số tiền hoàn
    COMPLETED,  // Đã hoàn tiền, cập nhật kho/đơn/VIP xong
    REJECTED,   // Từ chối xử lý hoàn trả

    // Giữ lại để tương thích dữ liệu/luồng cũ nếu hệ thống đã phát sinh trước đó
    PENDING,
    APPROVED,
    REFUNDED
}
