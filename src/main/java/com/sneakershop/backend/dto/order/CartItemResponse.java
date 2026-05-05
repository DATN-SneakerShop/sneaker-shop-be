package com.sneakershop.backend.dto.order;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemResponse {
    private Long itemId;
    private Long variantId;
    private Long productId;

    private String productName;
    private String sku;
    private String imageUrl;
    private String color;
    private String size;

    private Integer quantity;
    private Integer stock;
    private Boolean selected;

    /**
     * Giá gốc / 1 sản phẩm trước khuyến mãi
     */
    private BigDecimal originalUnitPrice;

    /**
     * Giá sale / 1 sản phẩm nếu đang có promotion active
     */
    private BigDecimal saleUnitPrice;

    /**
     * Giá đang hiển thị / 1 sản phẩm trong giỏ
     * = saleUnitPrice nếu có sale, ngược lại = originalUnitPrice
     */
    private BigDecimal unitPrice;

    /**
     * Tổng giảm giá của line item
     */
    private BigDecimal discountAmount;

    /**
     * Thành tiền của line item
     */
    private BigDecimal lineTotal;

    /**
     * Tên chương trình promotion đang áp dụng tốt nhất
     */
    private String promotionName;

    /**
     * Có đang sale không
     */
    private Boolean onSale;
}