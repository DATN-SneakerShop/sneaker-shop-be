package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.storefront.StorefrontOrderDetailResponse;
import com.sneakershop.backend.dto.order.storefront.StorefrontOrderItemResponse;
import com.sneakershop.backend.dto.order.storefront.StorefrontOrderSummaryResponse;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.login.UserRepository;
import com.sneakershop.backend.repository.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorefrontOrderQueryService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<StorefrontOrderSummaryResponse> getMyOrders(String principalName) {
        Customer customer = resolveCustomerByPrincipal(principalName);

        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .map(this::mapSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StorefrontOrderDetailResponse getMyOrderDetail(String principalName, Long orderId) {
        Customer customer = resolveCustomerByPrincipal(principalName);

        Order order = orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        return mapDetail(order);
    }

    private Customer resolveCustomerByPrincipal(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập");
        }

        User user = userRepository.findByUsername(principalName)
                .or(() -> userRepository.findByEmail(principalName))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản hiện tại"));

        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tài khoản chưa có hồ sơ khách hàng"));
    }

    private StorefrontOrderSummaryResponse mapSummary(Order order) {
        StorefrontOrderSummaryResponse dto = new StorefrontOrderSummaryResponse();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());
        dto.setLookupCode(order.getLookupCode());
        dto.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null);
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        dto.setShippingStatus(order.getShippingStatus() != null ? order.getShippingStatus().name() : null);
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        dto.setFinalAmount(order.getFinalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setReturnStatus(order.getReturnStatus() != null ? order.getReturnStatus().name() : null);

        int totalItems = order.getItems() == null
                ? 0
                : order.getItems().stream()
                  .map(OrderItem::getQuantity)
                  .filter(q -> q != null && q > 0)
                  .reduce(0, Integer::sum);

        dto.setTotalItems(totalItems);
        return dto;
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
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setReturnStatus(order.getReturnStatus() != null ? order.getReturnStatus().name() : null);
        dto.setUpdatedAt(order.getUpdatedAt());
        dto.setShippedAt(order.getShippedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setCancelledAt(order.getCancelledAt());
        dto.setReturnedAt(order.getReturnedAt());

        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream().map(this::mapItem).collect(Collectors.toList()));
        }

        return dto;
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