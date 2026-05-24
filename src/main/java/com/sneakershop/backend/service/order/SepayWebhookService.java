package com.sneakershop.backend.service.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sneakershop.backend.dto.order.CheckoutResponse;
import com.sneakershop.backend.dto.order.SepayWebhookRequest;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.PaymentTransaction;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.TransactionStatus;
import com.sneakershop.backend.entity.order.enums.TransactionType;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.repository.order.PaymentTransactionRepository;
import com.sneakershop.backend.service.notification.TelegramNotificationService;
import com.sneakershop.backend.service.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SepayService sepayService;
    private final TelegramNotificationService telegramNotificationService;
    private final ObjectMapper objectMapper;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public CheckoutResponse getPaymentInfo(String orderCode, String lookupCode) {
        Order order = orderRepository.findByOrderCodeAndDeletedFalse(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (lookupCode == null || !lookupCode.equals(order.getLookupCode())) {
            throw new RuntimeException("Lookup code không hợp lệ");
        }

        PaymentTransaction tx = paymentTransactionRepository
                .findTopByOrder_IdAndTransactionTypeOrderByCreatedAtDesc(order.getId(), TransactionType.PAYMENT)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giao dịch thanh toán"));

        String transferContent = tx.getIdempotencyKey();
        String qrImageUrl = sepayService.buildQrImageUrl(order.getFinalAmount(), transferContent);

        return CheckoutResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .lookupCode(order.getLookupCode())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .shippingStatus(order.getShippingStatus())
                .paymentMethod(order.getPaymentMethod())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .finalAmount(order.getFinalAmount())
                .paymentCode(tx.getIdempotencyKey())
                .bankCode(sepayService.getBankCode())
                .bankName(sepayService.getBankName())
                .bankAccountNo(sepayService.getBankAccountNo())
                .bankAccountName(sepayService.getAccountName())
                .transferContent(transferContent)
                .qrImageUrl(qrImageUrl)
                .message("Lấy thông tin thanh toán thành công")
                .build();
    }

    @Transactional
    public void handleWebhook(String authorizationHeader, SepayWebhookRequest request) {
        if (!sepayService.isValidWebhookAuth(authorizationHeader)) {
            telegramNotificationService.sendMessage(
                    "⚠️ <b>Webhook SePay lỗi</b>\n"
                            + "Lý do: Authorization không hợp lệ\n"
                            + "SePay ID: <b>" + safe(request != null ? request.getId() : null) + "</b>\n"
                            + "Content: <b>" + safe(request != null ? request.getContent() : null) + "</b>"
            );
            throw new RuntimeException("Webhook Authorization không hợp lệ");
        }

        if (request == null || request.getId() == null) {
            telegramNotificationService.sendMessage(
                    "⚠️ <b>Webhook SePay lỗi</b>\n"
                            + "Lý do: Thiếu transaction id từ SePay"
            );
            throw new RuntimeException("Webhook thiếu transaction id");
        }

        if (!"in".equalsIgnoreCase(request.getTransferType())) {
            log.info("Bỏ qua webhook vì không phải tiền vào. sepayId={}", request.getId());
            return;
        }

        if (!sepayService.matchesReceivingAccount(request.getAccountNumber(), request.getSubAccount())) {
            telegramNotificationService.sendMessage(
                    "⚠️ <b>Webhook SePay lỗi</b>\n"
                            + "Lý do: Sai tài khoản nhận tiền\n"
                            + "SePay ID: <b>" + safe(request.getId()) + "</b>\n"
                            + "Account: <b>" + safe(request.getAccountNumber()) + "</b>\n"
                            + "SubAccount: <b>" + safe(request.getSubAccount()) + "</b>"
            );
            throw new RuntimeException("Sai tài khoản nhận tiền");
        }

        PaymentTransaction duplicated = paymentTransactionRepository
                .findByProviderAndProviderTransactionId("SEPAY", String.valueOf(request.getId()))
                .orElse(null);

        if (duplicated != null) {
            telegramNotificationService.sendMessage(
                    "⚠️ <b>Webhook SePay trùng lặp</b>\n"
                            + "Lý do: Giao dịch đã được xử lý trước đó\n"
                            + "SePay ID: <b>" + request.getId() + "</b>\n"
                            + "Mã đơn: <b>" + safe(duplicated.getOrder() != null ? duplicated.getOrder().getOrderCode() : null) + "</b>\n"
                            + "Mã CK: <b>" + safe(duplicated.getIdempotencyKey()) + "</b>"
            );
            log.info("Webhook SePay đã xử lý trước đó. sepayId={}", request.getId());
            return;
        }

        String paymentCode = extractPaymentCode(request);
        if (paymentCode == null || paymentCode.isBlank()) {
            telegramNotificationService.sendMessage(
                    "⚠️ <b>Webhook SePay lỗi</b>\n"
                            + "Lý do: Không nhận diện được mã thanh toán\n"
                            + "SePay ID: <b>" + request.getId() + "</b>\n"
                            + "Content: <b>" + safe(request.getContent()) + "</b>"
            );
            throw new RuntimeException("Không nhận diện được mã thanh toán từ SePay");
        }

        PaymentTransaction tx = paymentTransactionRepository.findByIdempotencyKey(paymentCode)
                .orElseThrow(() -> {
                    telegramNotificationService.sendMessage(
                            "⚠️ <b>Webhook SePay lỗi</b>\n"
                                    + "Lý do: Không tìm thấy payment transaction theo mã thanh toán\n"
                                    + "SePay ID: <b>" + request.getId() + "</b>\n"
                                    + "Mã CK: <b>" + paymentCode + "</b>\n"
                                    + "Content: <b>" + safe(request.getContent()) + "</b>"
                    );
                    return new RuntimeException("Không tìm thấy payment transaction với mã: " + paymentCode);
                });

        Order order = tx.getOrder();

        if (!PaymentMethod.BANK_TRANSFER.equals(order.getPaymentMethod())) {
            telegramNotificationService.sendMessage(
                    "⚠️ <b>Webhook SePay lỗi</b>\n"
                            + "Lý do: Đơn hàng không dùng BANK_TRANSFER\n"
                            + "Mã đơn: <b>" + safe(order.getOrderCode()) + "</b>\n"
                            + "Payment method: <b>" + safe(order.getPaymentMethod()) + "</b>"
            );
            throw new RuntimeException("Đơn hàng không dùng BANK_TRANSFER");
        }

        if (TransactionStatus.SUCCESS.equals(tx.getStatus()) || PaymentStatus.PAID.equals(order.getPaymentStatus())) {
            tx.setProvider("SEPAY");
            tx.setProviderTransactionId(String.valueOf(request.getId()));
            tx.setProviderResponseCode(request.getReferenceCode());
            tx.setProviderMessage(request.getContent());
            tx.setRawPayload(toJson(request));
            paymentTransactionRepository.save(tx);

            telegramNotificationService.sendMessage(
                    "⚠️ <b>Webhook SePay trùng logic</b>\n"
                            + "Lý do: Đơn hàng đã ở trạng thái thanh toán thành công\n"
                            + "Mã đơn: <b>" + order.getOrderCode() + "</b>\n"
                            + "Mã CK: <b>" + safe(tx.getIdempotencyKey()) + "</b>\n"
                            + "SePay ID: <b>" + request.getId() + "</b>"
            );
            return;
        }

        BigDecimal incomingAmount = request.getTransferAmount() == null ? BigDecimal.ZERO : request.getTransferAmount();
        BigDecimal alreadyReceived = tx.getActualAmount() == null ? BigDecimal.ZERO : tx.getActualAmount();
        BigDecimal totalReceived = alreadyReceived.add(incomingAmount);
        BigDecimal expectedAmount = order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount();
        BigDecimal shortage = expectedAmount.subtract(totalReceived).max(BigDecimal.ZERO);
        BigDecimal excess = totalReceived.subtract(expectedAmount).max(BigDecimal.ZERO);

        tx.setProvider("SEPAY");
        tx.setProviderTransactionId(String.valueOf(request.getId()));
        tx.setProviderResponseCode(request.getReferenceCode());
        tx.setRawPayload(toJson(request));
        tx.setActualAmount(totalReceived);

        if (totalReceived.compareTo(expectedAmount) != 0) {
            String mismatchReason = "Chuyển khoản sai số tiền. Vui lòng liên hệ admin để được xử lý.";
            tx.setStatus(TransactionStatus.FAILED);
            tx.setProviderMessage(mismatchReason + " Cần=" + expectedAmount + " VND, thực nhận=" + totalReceived + " VND. Content=" + request.getContent());
            tx.setConfirmedAt(resolveReceivedAt(request));
            paymentTransactionRepository.save(tx);

            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setNote(appendNote(order.getNote(), mismatchReason));
            orderRepository.save(order);

            telegramNotificationService.sendMessage(
                    "⚠️ <b>Thanh toán SePay sai số tiền</b>\n"
                            + "Mã đơn: <b>" + order.getOrderCode() + "</b>\n"
                            + "Mã CK: <b>" + safe(tx.getIdempotencyKey()) + "</b>\n"
                            + "Cần: <b>" + expectedAmount + " VND</b>\n"
                            + "Thực nhận: <b>" + totalReceived + " VND</b>\n"
                            + (totalReceived.compareTo(expectedAmount) < 0 ? "Thiếu: <b>" + shortage + " VND</b>\n" : "Dư: <b>" + excess + " VND</b>\n")
                            + "SePay ID mới: <b>" + request.getId() + "</b>"
            );
            return;
        }

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setProviderMessage(request.getContent());
        tx.setConfirmedAt(resolveReceivedAt(request));
        paymentTransactionRepository.save(tx);

        order.setPaymentStatus(PaymentStatus.PAID);
        if (SalesChannel.OFFLINE.equals(order.getChannel())) {
            order.setOrderStatus(OrderStatus.COMPLETED);
            if (order.getCompletedAt() == null) {
                order.setCompletedAt(resolveReceivedAt(request));
            }
        } else if (OrderStatus.NEW.equals(order.getOrderStatus())) {
            order.setOrderStatus(OrderStatus.PROCESSING);
        }
        orderRepository.save(order);
        if (SalesChannel.OFFLINE.equals(order.getChannel()) && order.getCustomer() != null && OrderStatus.COMPLETED.equals(order.getOrderStatus())) {
            customerService.addPointsFromCompletedOrder(order.getCustomer().getId(), order.getFinalAmount(), order.getOrderCode());
        }

        telegramNotificationService.sendMessage(
                "✅ <b>Thanh toán SePay thành công</b>\n"
                        + "Mã đơn: <b>" + order.getOrderCode() + "</b>\n"
                        + "Mã CK: <b>" + safe(tx.getIdempotencyKey()) + "</b>\n"
                        + "Số tiền nhận lũy kế: <b>" + totalReceived + " VND</b>\n"
                        + "Ngân hàng: <b>" + safe(request.getGateway()) + "</b>\n"
                        + "Ref: <b>" + safe(request.getReferenceCode()) + "</b>\n"
                        + "SePay ID mới: <b>" + request.getId() + "</b>"
        );

        log.info("Đã xác nhận thanh toán SePay thành công cho orderCode={}, sepayId={}",
                order.getOrderCode(), request.getId());
    }

    private String extractPaymentCode(SepayWebhookRequest request) {
        if (request.getCode() != null && !request.getCode().isBlank()) {
            return request.getCode().trim();
        }

        String content = request.getContent();
        if (content == null || content.isBlank()) {
            return null;
        }

        String prefix = sepayService.getPaymentPrefix();
        int idx = content.toUpperCase().indexOf(prefix.toUpperCase());
        if (idx < 0) {
            return null;
        }

        String tail = content.substring(idx).trim().replaceAll("[^A-Za-z0-9_-]", "");
        return tail.isBlank() ? null : tail;
    }


    private LocalDateTime resolveReceivedAt(SepayWebhookRequest request) {
        if (request != null && request.getTransactionDate() != null && !request.getTransactionDate().isBlank()) {
            try {
                return LocalDateTime.parse(request.getTransactionDate().trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ignored) {
            }
            try {
                return LocalDateTime.parse(request.getTransactionDate().trim());
            } catch (Exception ignored) {
            }
        }
        return LocalDateTime.now();
    }

    private String toJson(Object data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return String.valueOf(data);
        }
    }

    private String appendNote(String oldNote, String message) {
        if (oldNote == null || oldNote.isBlank()) {
            return message;
        }
        if (oldNote.contains(message)) {
            return oldNote;
        }
        return oldNote + "\n" + message;
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}