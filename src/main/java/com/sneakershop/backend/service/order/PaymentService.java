package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.PaymentCallbackRequest;
import com.sneakershop.backend.dto.order.PaymentInitResponse;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.PaymentTransaction;
import com.sneakershop.backend.entity.order.enums.*;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.exception.PaymentException;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.repository.order.PaymentTransactionRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final List<PaymentGatewayClient> paymentGatewayClients;
    private final OrderInventoryService orderInventoryService;

    public PaymentInitResponse initPayment(Order order) {
        PaymentTransaction tx = paymentTransactionRepository.findTopByOrder_IdAndTransactionTypeOrderByCreatedAtDesc(order.getId(), TransactionType.PAYMENT)
                .orElseThrow(() -> new PaymentException("Không tìm thấy payment transaction pending cho order"));
        PaymentGatewayClient client = paymentGatewayClients.stream().filter(c -> c.supports(order.getPaymentMethod().name())).findFirst()
                .orElseThrow(() -> new PaymentException("Không tìm thấy gateway client phù hợp"));
        return client.createPaymentUrl(order, tx);
    }

    @Transactional
    public void handlePaymentCallback(PaymentCallbackRequest request) {
        if (request.getProvider() == null || request.getProvider().isBlank()) throw new PaymentException("Thiếu provider");
        PaymentGatewayClient client = paymentGatewayClients.stream().filter(c -> c.supports(request.getProvider())).findFirst()
                .orElseThrow(() -> new PaymentException("Provider không được hỗ trợ: " + request.getProvider()));
        if (!client.verifyCallback(request)) throw new PaymentException("Callback signature không hợp lệ");
        PaymentTransaction tx = resolveTransaction(request);
        if (TransactionStatus.SUCCESS.equals(tx.getStatus())) return;

        Order order = tx.getOrder();
        tx.setProvider(request.getProvider());
        tx.setProviderTransactionId(request.getProviderTransactionId());
        tx.setProviderResponseCode(request.getResponseCode());
        tx.setProviderMessage(request.getMessage());
        tx.setRawPayload(request.getRawPayload());

        if (Boolean.TRUE.equals(request.getSuccess())) {
            tx.setStatus(TransactionStatus.SUCCESS);
            tx.setActualAmount(request.getAmount() != null ? request.getAmount() : tx.getRequestAmount());
            tx.setConfirmedAt(LocalDateTime.now());
            paymentTransactionRepository.save(tx);
            order.setPaymentStatus(PaymentStatus.PAID);
            if (OrderStatus.NEW.equals(order.getOrderStatus())) order.setOrderStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);
        } else {
            tx.setStatus(TransactionStatus.FAILED);
            tx.setConfirmedAt(LocalDateTime.now());
            paymentTransactionRepository.save(tx);
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setOrderStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancelReason("Thanh toán online thất bại");
            // Thanh toán thất bại: hủy đơn và nhả tồn kho đã giữ chỗ qua service chuẩn để có lock + lịch sử kho.
            orderInventoryService.releaseForCancellation(order);
            orderRepository.save(order);
        }
    }

    @Transactional
    public void confirmBankTransfer(Long orderId) {
        Order order = orderRepository.findByIdAndDeletedFalse(orderId).orElseThrow(() -> new PaymentException("Không tìm thấy order"));
        PaymentTransaction tx = paymentTransactionRepository.findTopByOrder_IdAndTransactionTypeOrderByCreatedAtDesc(orderId, TransactionType.PAYMENT)
                .orElseThrow(() -> new PaymentException("Không tìm thấy transaction thanh toán"));
        if (!PaymentMethod.BANK_TRANSFER.equals(order.getPaymentMethod())) throw new PaymentException("Đơn hàng không dùng BANK_TRANSFER");
        if (TransactionStatus.SUCCESS.equals(tx.getStatus())) return;
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setActualAmount(order.getTotalAmount());
        tx.setProvider("BANK_TRANSFER");
        tx.setProviderMessage("Manual bank transfer confirmed");
        tx.setConfirmedAt(LocalDateTime.now());
        paymentTransactionRepository.save(tx);
        order.setPaymentStatus(PaymentStatus.PAID);
        if (OrderStatus.NEW.equals(order.getOrderStatus())) order.setOrderStatus(OrderStatus.PROCESSING);
        orderRepository.save(order);
    }

    @Transactional
    public void refund(Order order, BigDecimal amount, String reason, String provider) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) throw new PaymentException("Số tiền hoàn phải lớn hơn 0");
        PaymentTransaction refundTx = new PaymentTransaction();
        refundTx.setOrder(order);
        refundTx.setTransactionType(TransactionType.REFUND);
        refundTx.setPaymentMethod(order.getPaymentMethod());
        refundTx.setStatus(TransactionStatus.SUCCESS);
        refundTx.setRequestAmount(amount);
        refundTx.setActualAmount(amount);
        refundTx.setCurrencyCode(order.getCurrencyCode());
        refundTx.setProvider(provider != null ? provider : order.getPaymentMethod().name());
        refundTx.setProviderMessage(reason);
        refundTx.setConfirmedAt(LocalDateTime.now());
        paymentTransactionRepository.save(refundTx);
        BigDecimal returnedAmount = nz(order.getReturnedAmount()).add(amount);
        order.setReturnedAmount(returnedAmount);
        order.setFinalAmount(nz(order.getTotalAmount()).subtract(returnedAmount).max(BigDecimal.ZERO));
        order.setPaymentStatus(returnedAmount.compareTo(order.getTotalAmount()) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        orderRepository.save(order);
    }

    private PaymentTransaction resolveTransaction(PaymentCallbackRequest request) {
        if (request.getProviderTransactionId() != null && !request.getProviderTransactionId().isBlank()) {
            return paymentTransactionRepository.findByProviderAndProviderTransactionId(request.getProvider(), request.getProviderTransactionId())
                    .orElseGet(() -> resolveByRef(request));
        }
        return resolveByRef(request);
    }

    private PaymentTransaction resolveByRef(PaymentCallbackRequest request) {
        if (request.getTransactionRef() != null && !request.getTransactionRef().isBlank()) {
            return paymentTransactionRepository.findByIdempotencyKey(request.getTransactionRef())
                    .orElseThrow(() -> new PaymentException("Không tìm thấy transaction theo transactionRef"));
        }
        throw new PaymentException("Không đủ thông tin để xác định transaction callback");
    }

    private void restoreStock(Order order) {
        if (order.getItems() == null) return;
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

    private BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
