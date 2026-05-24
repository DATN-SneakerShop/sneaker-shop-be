package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.storefront.StorefrontOrderDetailResponse;
import com.sneakershop.backend.dto.order.storefront.StorefrontOrderItemResponse;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.entity.order.PaymentTransaction;
import com.sneakershop.backend.entity.order.enums.PaymentMethod;
import com.sneakershop.backend.entity.order.enums.PaymentStatus;
import com.sneakershop.backend.entity.order.enums.TransactionStatus;
import com.sneakershop.backend.entity.order.enums.TransactionType;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.repository.order.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StorefrontOrderLookupService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final SepayService sepayService;

    @Transactional(readOnly = true)
    public StorefrontOrderDetailResponse lookup(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng nhập mã đơn hàng hoặc mã tra cứu"
            );
        }

        String normalizedKeyword = keyword.trim();

        Order order = findOrder(normalizedKeyword);

        if (order == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy đơn hàng phù hợp"
            );
        }

        return mapDetail(order);
    }

    private Order findOrder(String keyword) {
        return orderRepository.findFirstByOrderCodeIgnoreCase(keyword)
                .or(() -> orderRepository.findFirstByLookupCodeIgnoreCase(keyword))
                .orElse(null);
    }

    private StorefrontOrderDetailResponse mapDetail(Order order) {
        StorefrontOrderDetailResponse dto = new StorefrontOrderDetailResponse();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setLookupCode(order.getLookupCode());

        dto.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        dto.setShippingStatus(order.getShippingStatus() != null ? order.getShippingStatus().name() : null);
        dto.setReturnStatus(order.getReturnStatus() != null ? order.getReturnStatus().name() : null);
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);

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

        dto.setShippingCarrier(order.getShippingCarrier());
        dto.setTrackingCode(order.getTrackingCode());

        dto.setNote(order.getNote());
        dto.setCancelReason(order.getCancelReason());
        dto.setReturnNote(order.getReturnNote());
        dto.setDeliveryFailReason(order.getDeliveryFailReason());

        dto.setSubtotalAmount(order.getSubtotalAmount());
        dto.setPromotionDiscountAmount(order.getPromotionDiscountAmount());
        dto.setVoucherDiscountAmount(order.getVoucherDiscountAmount());
        dto.setShippingDiscountAmount(order.getShippingDiscountAmount());
        dto.setManualDiscountAmount(order.getManualDiscountAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setShippingFee(order.getShippingFee());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setReturnedAmount(order.getReturnedAmount());
        dto.setFinalAmount(order.getFinalAmount());

        dto.setVoucherCode(order.getVoucherCode());
        dto.setVoucherNameSnapshot(order.getVoucherNameSnapshot());
        dto.setVoucherTypeSnapshot(order.getVoucherTypeSnapshot());
        dto.setVoucherValueSnapshot(order.getVoucherValueSnapshot());

        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setCancelledAt(order.getCancelledAt());
        dto.setReturnedAt(order.getReturnedAt());

        enrichPublicPaymentInfo(dto, order);

        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream().map(this::mapItem).collect(Collectors.toList()));
        }

        return dto;
    }

    private void enrichPublicPaymentInfo(StorefrontOrderDetailResponse dto, Order order) {
        if (!PaymentMethod.BANK_TRANSFER.equals(order.getPaymentMethod())) {
            return;
        }

        BigDecimal expectedAmount = order.getFinalAmount() == null ? BigDecimal.ZERO : order.getFinalAmount();
        dto.setPaymentExpectedAmount(expectedAmount);

        PaymentTransaction tx = paymentTransactionRepository
                .findTopByOrder_IdAndTransactionTypeOrderByCreatedAtDesc(order.getId(), TransactionType.PAYMENT)
                .orElseGet(() -> {
                    PaymentTransaction created = new PaymentTransaction();
                    created.setOrder(order);
                    created.setTransactionType(TransactionType.PAYMENT);
                    created.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
                    created.setStatus(TransactionStatus.PENDING);
                    created.setRequestAmount(expectedAmount);
                    created.setActualAmount(BigDecimal.ZERO);
                    created.setProvider("SEPAY");
                    created.setIdempotencyKey(sepayService.buildPaymentCode(order.getId()));
                    return created;
                });

        dto.setPaymentActualAmount(tx.getActualAmount() == null ? BigDecimal.ZERO : tx.getActualAmount());
        dto.setPaymentCode(tx.getIdempotencyKey());
        dto.setTransferContent(tx.getIdempotencyKey());
        dto.setBankCode(sepayService.getBankCode());
        dto.setBankName(sepayService.getBankName());
        dto.setBankAccountNo(sepayService.getBankAccountNo());
        dto.setBankAccountName(sepayService.getAccountName());

        if (PaymentStatus.FAILED.equals(order.getPaymentStatus()) || TransactionStatus.FAILED.equals(tx.getStatus())) {
            dto.setPaymentErrorMessage("Thanh toán chuyển khoản lỗi. Bạn đã chuyển sai số tiền. Vui lòng liên hệ admin để được xử lý.");
            return;
        }

        if ((PaymentStatus.UNPAID.equals(order.getPaymentStatus()) || PaymentStatus.PENDING.equals(order.getPaymentStatus()))
                && sepayService.isEnabled()) {
            dto.setQrImageUrl(sepayService.buildQrImageUrl(expectedAmount, tx.getIdempotencyKey()));
        }
    }

    private StorefrontOrderItemResponse mapItem(OrderItem item) {
        StorefrontOrderItemResponse dto = new StorefrontOrderItemResponse();
        dto.setId(item.getId());
        dto.setVariantIdSnapshot(item.getVariantIdSnapshot());
        dto.setSkuSnapshot(item.getSkuSnapshot());
        dto.setProductNameSnapshot(item.getProductNameSnapshot());
        dto.setColorSnapshot(item.getColorSnapshot());
        dto.setSizeSnapshot(item.getSizeSnapshot());
        dto.setMaterialSnapshot(item.getMaterialSnapshot());
        dto.setSoleSnapshot(item.getSoleSnapshot());
        dto.setImageUrlSnapshot(item.getImageUrlSnapshot());
        dto.setBaseUnitPrice(item.getBaseUnitPrice());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setQuantity(item.getQuantity());
        dto.setPromotionDiscountAmount(item.getPromotionDiscountAmount());
        dto.setLineDiscountAmount(item.getLineDiscountAmount());
        dto.setLineTotalAmount(item.getLineTotalAmount());
        return dto;
    }
}