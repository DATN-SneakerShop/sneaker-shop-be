package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.CancelOrderRequest;
import com.sneakershop.backend.dto.order.ReturnOrderRequest;
import com.sneakershop.backend.dto.order.UpdateOrderStatusRequest;
import com.sneakershop.backend.dto.order.admin.AdminOrderDetailDTO;
import com.sneakershop.backend.dto.order.admin.AdminOrderPaymentHistoryDTO;
import com.sneakershop.backend.dto.order.admin.AdminOrderItemDTO;
import com.sneakershop.backend.dto.order.admin.AdminOrderSummaryDTO;
import com.sneakershop.backend.dto.order.admin.CounterPaymentQrResponse;
import com.sneakershop.backend.dto.order.admin.AdminOrderUpdateRequest;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.PaymentTransaction;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.ShippingStatus;
import com.sneakershop.backend.entity.order.enums.TransactionStatus;
import com.sneakershop.backend.entity.order.enums.TransactionType;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.repository.order.PaymentTransactionRepository;
import com.sneakershop.backend.service.notification.TelegramNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderManagementService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderInventoryService orderInventoryService;
    private final TelegramNotificationService telegramNotificationService;
    private final SepayService sepayService;

    @Transactional(readOnly = true)
    public List<AdminOrderSummaryDTO> list(
            String keyword,
            OrderStatus orderStatus,
            PaymentStatus paymentStatus,
            ShippingStatus shippingStatus,
            PaymentMethod paymentMethod,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return orderRepository.findAllByDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .filter(order -> matchesKeyword(order, keyword))
                .filter(order -> orderStatus == null || orderStatus == order.getOrderStatus())
                .filter(order -> paymentStatus == null || paymentStatus == order.getPaymentStatus())
                .filter(order -> shippingStatus == null || shippingStatus == order.getShippingStatus())
                .filter(order -> paymentMethod == null || paymentMethod == order.getPaymentMethod())
                .filter(order -> matchesDate(order.getCreatedAt(), dateFrom, dateTo))
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailDTO detail(Long id) {
        Order order = getOrderOrThrow(id);
        return toDetailDto(order);
    }

    @Transactional
    public CounterPaymentQrResponse getCounterPaymentQr(Long id) {
        Order order = getOrderOrThrow(id);

        if (order.getChannel() != SalesChannel.OFFLINE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ tạo QR cho đơn nhận tại quầy");
        }
        if (order.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn này không sử dụng thanh toán chuyển khoản");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn đã bị hủy");
        }
        if (!sepayService.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SePay chưa được bật cấu hình");
        }

        PaymentTransaction tx = paymentTransactionRepository
                .findTopByOrder_IdAndTransactionTypeOrderByCreatedAtDesc(order.getId(), TransactionType.PAYMENT)
                .orElseGet(() -> {
                    PaymentTransaction created = new PaymentTransaction();
                    created.setOrder(order);
                    created.setTransactionType(TransactionType.PAYMENT);
                    created.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
                    created.setStatus(TransactionStatus.PENDING);
                    created.setRequestAmount(order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount());
                    created.setActualAmount(BigDecimal.ZERO);
                    created.setProvider("SEPAY");
                    created.setIdempotencyKey(sepayService.buildPaymentCode(order.getId()));
                    return paymentTransactionRepository.save(created);
                });

        if (tx.getPaymentMethod() == null) {
            tx.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        }
        if (tx.getRequestAmount() == null || tx.getRequestAmount().compareTo(BigDecimal.ZERO) <= 0) {
            tx.setRequestAmount(order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount());
        }
        if (tx.getActualAmount() == null) {
            tx.setActualAmount(BigDecimal.ZERO);
        }
        if (tx.getProvider() == null || tx.getProvider().isBlank()) {
            tx.setProvider("SEPAY");
        }
        if (tx.getIdempotencyKey() == null || tx.getIdempotencyKey().isBlank()) {
            tx.setIdempotencyKey(sepayService.buildPaymentCode(order.getId()));
        }
        paymentTransactionRepository.save(tx);

        BigDecimal amount = order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount();
        String transferContent = tx.getIdempotencyKey();

        return CounterPaymentQrResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .amount(amount)
                .paymentCode(tx.getIdempotencyKey())
                .bankCode(sepayService.getBankCode())
                .bankName(sepayService.getBankName())
                .bankAccountNo(sepayService.getBankAccountNo())
                .accountName(sepayService.getAccountName())
                .transferContent(transferContent)
                .qrImageUrl(sepayService.buildQrImageUrl(amount, transferContent))
                .build();
    }

    @Transactional
    public AdminOrderDetailDTO updateMeta(Long id, AdminOrderUpdateRequest request) {
        Order order = getOrderOrThrow(id);

        if (request.getPaymentStatus() != null) {
            order.setPaymentStatus(request.getPaymentStatus());

            if (request.getPaymentStatus() == PaymentStatus.PAID) {
                if (order.getChannel() == SalesChannel.OFFLINE) {
                    if (order.getOrderStatus() != OrderStatus.COMPLETED) {
                        orderInventoryService.commitReservedStock(order);
                    }
                    order.setOrderStatus(OrderStatus.COMPLETED);
                    if (order.getCompletedAt() == null) {
                        order.setCompletedAt(LocalDateTime.now());
                    }
                } else if (order.getOrderStatus() == OrderStatus.NEW) {
                    order.setOrderStatus(OrderStatus.PROCESSING);
                }
            }
        }

        if (request.getShippingStatus() != null) {
            ShippingStatus previous = order.getShippingStatus();
            ShippingStatus next = request.getShippingStatus();
            order.setShippingStatus(next);

            // THÊM MỚI: Nếu cập nhật thành Sẵn sàng giao, tự chuyển trạng thái đơn sang Đang xử lý
            if (next == ShippingStatus.READY_TO_SHIP && order.getOrderStatus() == OrderStatus.NEW) {
                order.setOrderStatus(OrderStatus.PROCESSING);
            }

            if (next == ShippingStatus.SHIPPED && order.getShippedAt() == null) {
                order.setShippedAt(LocalDateTime.now());
                // SỬA: Cho phép chuyển sang Đang giao (SHIPPING) từ cả Đang xử lý hoặc Mới
                if (order.getOrderStatus() == OrderStatus.PROCESSING || order.getOrderStatus() == OrderStatus.NEW) {
                    order.setOrderStatus(OrderStatus.SHIPPING);
                }
            }
            if (next == ShippingStatus.DELIVERED && order.getDeliveredAt() == null) {
                order.setDeliveredAt(LocalDateTime.now());
                if (order.getOrderStatus() != OrderStatus.COMPLETED) {
                    orderInventoryService.commitReservedStock(order);
                    order.setOrderStatus(OrderStatus.COMPLETED);
                    if (order.getCompletedAt() == null) {
                        order.setCompletedAt(LocalDateTime.now());
                    }
                    if (order.getPaymentMethod() == PaymentMethod.COD && order.getPaymentStatus() != PaymentStatus.PAID) {
                        order.setPaymentStatus(PaymentStatus.PAID);
                    }
                    telegramNotificationService.sendMessage(
                            "✅ <b>Đơn hàng hoàn thành</b>\n"
                                    + "Mã đơn: <b>" + order.getOrderCode() + "</b>\n"
                                    + "Khách: <b>" + blankSafe(order.getOrdererName()) + "</b>\n"
                                    + "Tổng tiền: <b>" + (order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount()) + " VND</b>\n"
                                    + "Thanh toán: <b>" + String.valueOf(order.getPaymentStatus()) + "</b>"
                    );
                }
            }
            if (next == ShippingStatus.DELIVERY_FAILED && order.getDeliveryFailedAt() == null) {
                order.setDeliveryFailedAt(LocalDateTime.now());
            }
            if (next != ShippingStatus.DELIVERY_FAILED && previous == ShippingStatus.DELIVERY_FAILED) {
                order.setDeliveryFailReason(null);
            }
        }

        if (request.getShippingCarrier() != null) {
            order.setShippingCarrier(blankToNull(request.getShippingCarrier()));
        }
        if (request.getTrackingCode() != null) {
            order.setTrackingCode(blankToNull(request.getTrackingCode()));
        }
        if (request.getDeliveryFailReason() != null) {
            order.setDeliveryFailReason(blankToNull(request.getDeliveryFailReason()));
        }
        if (request.getNote() != null) {
            order.setNote(request.getNote());
        }

        return toDetailDto(orderRepository.save(order));
    }

    @Transactional
    public AdminOrderDetailDTO updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        orderService.updateStatus(id, request);
        return detail(id);
    }

    @Transactional
    public AdminOrderDetailDTO cancel(Long id, CancelOrderRequest request) {
        orderService.cancel(id, request);
        return detail(id);
    }

    @Transactional
    public AdminOrderDetailDTO applyReturn(Long id, ReturnOrderRequest request) {
        orderService.applyReturn(id, request);
        return detail(id);
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng: " + id));
    }

    private boolean matchesDate(LocalDateTime createdAt, LocalDate from, LocalDate to) {
        if (createdAt == null) return false;
        LocalDate date = createdAt.toLocalDate();
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }

    private boolean matchesKeyword(Order order, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String needle = keyword.trim().toLowerCase(Locale.ROOT);

        return contains(order.getOrderCode(), needle)
                || contains(order.getLookupCode(), needle)
                || contains(order.getOrdererName(), needle)
                || contains(order.getOrdererEmail(), needle)
                || contains(order.getOrdererPhone(), needle)
                || contains(order.getReceiverName(), needle)
                || contains(order.getReceiverPhone(), needle)
                || contains(order.getTrackingCode(), needle);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String blankSafe(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int sumItemQuantity(Order order) {
        if (order.getItems() == null) return 0;
        return order.getItems()
                .stream()
                .filter(Objects::nonNull)
                .map(OrderItem::getQuantity)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private AdminOrderSummaryDTO toSummaryDto(Order order) {
        AdminOrderSummaryDTO dto = new AdminOrderSummaryDTO();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setLookupCode(order.getLookupCode());
        dto.setCreatedAt(order.getCreatedAt());

        dto.setCustomerId(order.getCustomer() != null ? order.getCustomer().getId() : null);
        dto.setGuestOrder(order.getGuestOrder());
        dto.setOrdererName(order.getOrdererName());
        dto.setOrdererEmail(order.getOrdererEmail());
        dto.setOrdererPhone(order.getOrdererPhone());

        dto.setReceiverName(order.getReceiverName());
        dto.setReceiverPhone(order.getReceiverPhone());
        dto.setItemCount(sumItemQuantity(order));

        dto.setTotalAmount(order.getTotalAmount());
        dto.setFinalAmount(order.getFinalAmount());

        dto.setChannel(order.getChannel());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setShippingStatus(order.getShippingStatus());
        return dto;
    }

    private AdminOrderDetailDTO toDetailDto(Order order) {
        AdminOrderDetailDTO dto = new AdminOrderDetailDTO();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setLookupCode(order.getLookupCode());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        dto.setOrderStatus(order.getOrderStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setShippingStatus(order.getShippingStatus());
        dto.setReturnStatus(order.getReturnStatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setChannel(order.getChannel());

        dto.setCustomerId(order.getCustomer() != null ? order.getCustomer().getId() : null);
        dto.setCreatedById(order.getCreatedBy() != null ? order.getCreatedBy().getId() : null);
        dto.setGuestOrder(order.getGuestOrder());

        dto.setOrdererName(order.getOrdererName());
        dto.setOrdererEmail(order.getOrdererEmail());
        dto.setOrdererPhone(order.getOrdererPhone());

        dto.setReceiverName(order.getReceiverName());
        dto.setReceiverPhone(order.getReceiverPhone());
        dto.setAddressLabel(order.getAddressLabel());
        dto.setShippingProvince(order.getShippingProvince());
        dto.setShippingDistrict(order.getShippingDistrict());
        dto.setShippingWard(order.getShippingWard());
        dto.setShippingDetailAddress(order.getShippingDetailAddress());
        dto.setShippingAddressLine(order.getShippingAddressLine());

        dto.setSubtotalAmount(order.getSubtotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setPromotionDiscountAmount(order.getPromotionDiscountAmount());
        dto.setVoucherDiscountAmount(order.getVoucherDiscountAmount());
        dto.setShippingDiscountAmount(order.getShippingDiscountAmount());
        dto.setManualDiscountAmount(order.getManualDiscountAmount());
        dto.setShippingFee(order.getShippingFee());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setFinalAmount(order.getFinalAmount());
        dto.setReturnedAmount(order.getReturnedAmount());

        List<PaymentTransaction> paymentTransactions = paymentTransactionRepository
                .findAllByOrder_IdAndTransactionTypeOrderByCreatedAtAsc(order.getId(), TransactionType.PAYMENT);
        PaymentTransaction latestPayment = paymentTransactions.isEmpty() ? null : paymentTransactions.get(paymentTransactions.size() - 1);
        dto.setReceivedAmount(latestPayment != null ? latestPayment.getActualAmount() : null);
        dto.setPaymentActualAmount(latestPayment != null ? latestPayment.getActualAmount() : null);
        dto.setLastTransferReceivedAt(latestPayment != null ? latestPayment.getConfirmedAt() : null);
        dto.setPaymentReceivedAt(latestPayment != null ? latestPayment.getConfirmedAt() : null);
        dto.setPaymentHistory(paymentTransactions.stream().map(this::toPaymentHistoryDto).collect(Collectors.toList()));

        if (PaymentMethod.BANK_TRANSFER.equals(order.getPaymentMethod())) {
            dto.setBankName(sepayService.getBankName());
            dto.setBankCode(sepayService.getBankCode());
            dto.setBankAccountNo(sepayService.getBankAccountNo());
            dto.setBankAccountName(sepayService.getAccountName());
            dto.setPaymentCode(latestPayment != null ? latestPayment.getIdempotencyKey() : sepayService.buildPaymentCode(order.getId()));
            dto.setQrImageUrl(sepayService.buildQrImageUrl(order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount(), dto.getPaymentCode()));
        }

        dto.setVoucherCode(order.getVoucherCode());
        dto.setVoucherNameSnapshot(order.getVoucherNameSnapshot());
        dto.setVoucherTypeSnapshot(order.getVoucherTypeSnapshot());
        dto.setVoucherValueSnapshot(order.getVoucherValueSnapshot());
        dto.setPromotionCode(order.getPromotionCode());
        dto.setAppliedPromotionSummary(order.getAppliedPromotionSummary());

        dto.setShippingCarrier(order.getShippingCarrier());
        dto.setTrackingCode(order.getTrackingCode());
        dto.setDeliveryFailReason(order.getDeliveryFailReason());

        dto.setNote(order.getNote());
        dto.setCancelReason(order.getCancelReason());
        dto.setReturnNote(order.getReturnNote());

        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setDeliveryFailedAt(order.getDeliveryFailedAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setCancelledAt(order.getCancelledAt());
        dto.setReturnedAt(order.getReturnedAt());

        dto.setEmailSent(order.getEmailSent());
        dto.setEmailSentAt(order.getEmailSentAt());

        List<AdminOrderItemDTO> items = order.getItems() == null ? Collections.emptyList() : order.getItems()
                .stream()
                .map(item -> {
                    AdminOrderItemDTO itemDto = new AdminOrderItemDTO();
                    itemDto.setId(item.getId());
                    itemDto.setVariantId(item.getVariantId());
                    itemDto.setProductIdSnapshot(item.getProductIdSnapshot());
                    itemDto.setVariantIdSnapshot(item.getVariantIdSnapshot());
                    itemDto.setImageUrlSnapshot(item.getImageUrlSnapshot());
                    itemDto.setProductNameSnapshot(item.getProductNameSnapshot());
                    itemDto.setSkuSnapshot(item.getSkuSnapshot());
                    itemDto.setColorSnapshot(item.getColorSnapshot());
                    itemDto.setSizeSnapshot(item.getSizeSnapshot());
                    itemDto.setMaterialSnapshot(item.getMaterialSnapshot());
                    itemDto.setSoleSnapshot(item.getSoleSnapshot());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setReturnedQuantity(item.getReturnedQuantity());
                    itemDto.setBaseUnitPrice(item.getBaseUnitPrice());
                    itemDto.setUnitPrice(item.getUnitPrice());
                    itemDto.setPromotionDiscountAmount(item.getPromotionDiscountAmount());
                    itemDto.setLineDiscountAmount(item.getLineDiscountAmount());
                    itemDto.setLineTotalAmount(item.getLineTotalAmount());
                    itemDto.setReturnNote(item.getReturnNote());
                    return itemDto;
                })
                .collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }

    private AdminOrderPaymentHistoryDTO toPaymentHistoryDto(PaymentTransaction tx) {
        AdminOrderPaymentHistoryDTO dto = new AdminOrderPaymentHistoryDTO();
        dto.setTransactionId(tx.getId());
        dto.setProvider(tx.getProvider());
        dto.setProviderTransactionId(tx.getProviderTransactionId());
        dto.setReferenceCode(tx.getProviderResponseCode());
        dto.setMessage(tx.getProviderMessage());
        dto.setAmount(tx.getActualAmount());
        dto.setActualAmount(tx.getActualAmount());
        dto.setTransferAmount(tx.getActualAmount());
        dto.setRunningAmount(tx.getActualAmount());
        dto.setCreatedAt(tx.getCreatedAt());
        dto.setConfirmedAt(tx.getConfirmedAt());
        dto.setReceivedAt(tx.getConfirmedAt());
        return dto;
    }

}
