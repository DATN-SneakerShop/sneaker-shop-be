package com.sneakershop.backend.entity.order.enums;

/**
 * Nơi đi/luồng xử lý cuối cùng của hàng hoàn sau khi shop nhận và duyệt.
 */
public enum ReturnDispositionType {
    /** Hàng còn mới, nhập lại kho bán bình thường. */
    RESTOCKED,

    /** Hàng bị khách làm hỏng, không nhập lại kho bán. */
    DAMAGED_BY_CUSTOMER,

    /** Hàng lỗi do sản xuất/nhà cung cấp, không nhập lại kho bán. */
    MANUFACTURER_DEFECT,

    /** Hàng lỗi/hỏng đang chờ sửa chữa. */
    REPAIR_PENDING,

    /** Hàng lỗi chờ gửi/đối soát với nhà cung cấp. */
    SUPPLIER_CLAIM,

    /** Hàng không bán lại được, đang giữ ở khu/kho hàng lỗi. */
    NOT_RESELLABLE_HOLD,

    /** Hàng đã/chuẩn bị hủy bỏ. */
    DISPOSED,

    /** Hàng không đủ điều kiện bán mới, chờ bán thanh lý. */
    LIQUIDATION
}
