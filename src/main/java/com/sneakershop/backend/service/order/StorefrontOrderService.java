package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.*;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.enums.*;
import com.sneakershop.backend.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorefrontOrderService {
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final OrderInventoryService orderInventoryService;

    @Transactional
    public void cancelOrder(Long orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndDeletedFalse(orderId).orElseThrow(() -> new RuntimeException("Không tìm thấy order"));
        if (OrderStatus.CANCELLED.equals(order.getOrderStatus())) return;
        if (OrderStatus.COMPLETED.equals(order.getOrderStatus())) throw new RuntimeException("Không thể hủy đơn đã hoàn tất");
        if (ShippingStatus.SHIPPED.equals(order.getShippingStatus()) || ShippingStatus.DELIVERED.equals(order.getShippingStatus())) {
            throw new RuntimeException("Không thể hủy đơn đã giao vận");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(request.getReason());
        // Luôn nhả tồn kho qua OrderInventoryService để có lock và ghi lịch sử kho.
        orderInventoryService.releaseForCancellation(order);
        if (PaymentStatus.PAID.equals(order.getPaymentStatus())) {
            paymentService.refund(order, order.getTotalAmount(), "Refund due to order cancellation", "MANUAL");
        } else if (PaymentMethod.COD.equals(order.getPaymentMethod())) {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        } else {
            order.setPaymentStatus(PaymentStatus.FAILED);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void markDelivered(Long orderId) {
        Order order = orderRepository.findByIdAndDeletedFalse(orderId).orElseThrow(() -> new RuntimeException("Không tìm thấy order"));
        throw new RuntimeException("Khách hàng không được tự đánh dấu đơn đã giao thành công. Vui lòng xử lý giao hàng tại màn hình quản trị.");
    }

    @Transactional
    public void refundOrder(Long orderId, RefundRequest request) {
        throw new RuntimeException("Khách hàng không được tự hoàn tiền đơn hàng. Vui lòng tạo yêu cầu trả hàng/hoàn tiền hoặc liên hệ admin.");
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findByIdAndDeletedFalse(orderId).orElseThrow(() -> new RuntimeException("Không tìm thấy order"));
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .lookupCode(order.getLookupCode())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .shippingStatus(order.getShippingStatus())
                .returnStatus(order.getReturnStatus())
                .paymentMethod(order.getPaymentMethod())
                .ordererName(order.getOrdererName())
                .ordererEmail(order.getOrdererEmail())
                .ordererPhone(order.getOrdererPhone())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingAddressLine(order.getShippingAddressLine())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .returnedAmount(order.getReturnedAmount())
                .finalAmount(order.getFinalAmount())
                .voucherCode(order.getVoucherCode())
                .promotionCode(order.getPromotionCode())
                .shippingCarrier(order.getShippingCarrier())
                .trackingCode(order.getTrackingCode())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .items(order.getItems() == null ? Collections.emptyList() : order.getItems().stream().map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .variantId(item.getVariantId())
                        .skuSnapshot(item.getSkuSnapshot())
                        .productNameSnapshot(item.getProductNameSnapshot())
                        .colorSnapshot(item.getColorSnapshot())
                        .sizeSnapshot(item.getSizeSnapshot())
                        .materialSnapshot(item.getMaterialSnapshot())
                        .soleSnapshot(item.getSoleSnapshot())
                        .imageUrlSnapshot(item.getImageUrlSnapshot())
                        .baseUnitPrice(item.getBaseUnitPrice())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .promotionDiscountAmount(item.getPromotionDiscountAmount())
                        .lineDiscountAmount(item.getLineDiscountAmount())
                        .lineTotalAmount(item.getLineTotalAmount())
                        .returnedQuantity(item.getReturnedQuantity())
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
