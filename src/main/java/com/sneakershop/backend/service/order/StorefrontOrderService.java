package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.*;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.enums.*;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
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
    private final ProductVariantRepository productVariantRepository;
    private final PaymentService paymentService;

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
        if (order.getItems() != null) {
            order.getItems().forEach(item -> {
                if (item.getVariant() != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    ProductVariant variant = item.getVariant();
                    int currentReserved = Math.max(variant.getReserved_quantity(), 0);

                    variant.setReserved_quantity(
                            Math.max(currentReserved - item.getQuantity(), 0)
                    );
                    productVariantRepository.save(variant);
                    productVariantRepository.save(item.getVariant());
                }
            });
        }
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
        order.setShippingStatus(ShippingStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        if (PaymentMethod.COD.equals(order.getPaymentMethod()) && !PaymentStatus.PAID.equals(order.getPaymentStatus())) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void refundOrder(Long orderId, RefundRequest request) {
        Order order = orderRepository.findByIdAndDeletedFalse(orderId).orElseThrow(() -> new RuntimeException("Không tìm thấy order"));
        if (!PaymentStatus.PAID.equals(order.getPaymentStatus()) && !PaymentStatus.PARTIALLY_REFUNDED.equals(order.getPaymentStatus())) {
            throw new RuntimeException("Chỉ hoàn tiền cho đơn đã thanh toán");
        }
        paymentService.refund(order, request.getAmount(), request.getReason(), request.getProvider());
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
