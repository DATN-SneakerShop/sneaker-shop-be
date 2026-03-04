package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.*;
import com.sneakershop.backend.entity.customer.KhachHang;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.entity.order.enums.*;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.entity.promotion.Promotion;
import com.sneakershop.backend.repository.customer.KhachHangRepository;
import com.sneakershop.backend.repository.login.UserRepository;
import com.sneakershop.backend.repository.order.OrderItemRepository;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import com.sneakershop.backend.repository.promotion.PromotionRepository;
import com.sneakershop.backend.service.pricing.VariantPriceGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final ProductVariantRepository variantRepository;
    private final KhachHangRepository khachHangRepository;
    private final UserRepository userRepository;

    private final VariantPriceGroupService variantPriceGroupService;
    private final PromotionRepository promotionRepository;

    // ====================== CREATE ORDER ======================
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {

        User currentUser = getCurrentUser();

        KhachHang customer = null;
        String loaiKhach = "NORMAL";

        if (request.getCustomerId() != null) {
            customer = khachHangRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
            if (customer.getLoaiKhach() != null) {
                loaiKhach = customer.getLoaiKhach();
            }
        }

        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setCustomer(customer);
        order.setCreatedBy(currentUser);

        order.setChannel(Optional.ofNullable(request.getChannel()).orElse(SalesChannel.OFFLINE));
        order.setPaymentMethod(Optional.ofNullable(request.getPaymentMethod()).orElse(PaymentMethod.COD));

        // PaidNow chỉ dùng như “thu tiền tại quầy”
        if (Boolean.TRUE.equals(request.getPaidNow())) {
            order.setPaymentStatus(PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        }

        order.setShippingFee(nvl(request.getShippingFee()));
        order.setShippingCarrier(request.getShippingCarrier());
        order.setTrackingCode(request.getTrackingCode());
        order.setNote(request.getNote());
        order.setCurrencyCode(Optional.ofNullable(request.getCurrencyCode()).orElse("VND"));

        // ===== Items + tính tiền + trừ kho =====
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;

        for (CreateOrderItemRequest itemReq : request.getItems()) {

            ProductVariant variant = variantRepository.findById(itemReq.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm (variantId=" + itemReq.getVariantId() + ")"));

            int qty = Optional.ofNullable(itemReq.getQuantity()).orElse(0);
            if (qty <= 0) throw new RuntimeException("Số lượng phải >= 1");

            if (variant.getStock() < qty) {
                throw new RuntimeException("Không đủ tồn kho cho SKU " + variant.getSku() + ". Tồn: " + variant.getStock());
            }

            // 1) Giá gốc theo nhóm khách
            BigDecimal baseUnitPrice = variantPriceGroupService.getPriceByCustomerType(variant.getId(), loaiKhach);

            // 2) Áp promotion tốt nhất (theo quantity)
            PriceCalcResult calc = applyBestPromotion(variant.getId(), baseUnitPrice, qty, loaiKhach);

            BigDecimal lineOriginal = baseUnitPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal lineDiscount = lineOriginal.subtract(calc.getFinalTotal()).max(BigDecimal.ZERO);
            BigDecimal lineFinal = calc.getFinalTotal();

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setVariant(variant);

            oi.setSkuSnapshot(variant.getSku());
            oi.setProductNameSnapshot(variant.getProduct() != null ? variant.getProduct().getName() : null);

            oi.setUnitPrice(baseUnitPrice);
            oi.setQuantity(qty);
            oi.setLineDiscountAmount(lineDiscount);
            oi.setLineTotalAmount(lineFinal);

            order.getItems().add(oi);

            subtotal = subtotal.add(lineOriginal);
            discount = discount.add(lineDiscount);

            // trừ kho ngay khi tạo đơn
            variant.setStock(variant.getStock() - qty);
            if (variant.getStock() <= 0) {
                variant.setStock(0);
                variant.setStatus("OUT_OF_STOCK");
            } else {
                variant.setStatus("IN_STOCK");
            }
            variantRepository.save(variant);
        }

        order.setSubtotalAmount(subtotal);
        order.setDiscountAmount(discount);

        BigDecimal total = subtotal.subtract(discount).add(order.getShippingFee());
        order.setTotalAmount(total.max(BigDecimal.ZERO));

        // snapshot final (chưa hoàn)
        order.setFinalAmount(order.getTotalAmount());
        order.setReturnedAmount(BigDecimal.ZERO);
        order.setPromotionDiscountAmount(BigDecimal.ZERO);

        Order saved = orderRepository.save(order);
        return toDetailResponse(saved);
    }
    // ====================== DETAIL ======================
    @Transactional(readOnly = true)
    public OrderResponse getDetail(Long id) {
        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        return toDetailResponse(order);
    }
    // ====================== SEARCH (LIST) ======================
    @Transactional(readOnly = true)
    public Page<OrderResponse> search(
            String keyword,
            OrderStatus status,
            SalesChannel channel,
            PaymentStatus paymentStatus,
            ReturnStatus returnStatus,
            Long customerId,
            Long createdById,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        return orderRepository.search(
                        keyword,
                        status,
                        channel,
                        paymentStatus,
                        returnStatus,
                        customerId,
                        createdById,
                        fromDate,
                        toDate,
                        pageable
                )
                .map(this::toSummaryResponse);
    }

    // ====================== UPDATE STATUS ======================
    @Transactional
    public OrderResponse updateStatus(Long id, UpdateOrderStatusRequest request) {

        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        OrderStatus newStatus = request.getStatus();
        if (newStatus == null) throw new RuntimeException("Thiếu trạng thái");

        // validate state transition (đủ dùng cho đồ án)
        OrderStatus current = order.getOrderStatus();

        if (current == OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn đã hủy, không thể cập nhật trạng thái");
        }
        if (current == OrderStatus.COMPLETED) {
            throw new RuntimeException("Đơn đã hoàn tất, không thể cập nhật trạng thái");
        }

        if (newStatus == OrderStatus.CANCELLED) {
            // yêu cầu dùng endpoint cancel để có lý do + hoàn kho
            throw new RuntimeException("Vui lòng dùng /cancel để hủy đơn");
        }

        // NEW -> PROCESSING -> SHIPPING -> COMPLETED
        if (!isValidTransition(current, newStatus)) {
            throw new RuntimeException("Chuyển trạng thái không hợp lệ: " + current + " -> " + newStatus);
        }

        order.setOrderStatus(newStatus);

        if (newStatus == OrderStatus.SHIPPING) {
            order.setShippedAt(LocalDateTime.now());
        }
        if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        return toDetailResponse(orderRepository.save(order));
    }

    private boolean isValidTransition(OrderStatus current, OrderStatus target) {
        if (current == OrderStatus.NEW && target == OrderStatus.PROCESSING) return true;
        if (current == OrderStatus.PROCESSING && (target == OrderStatus.SHIPPING)) return true;
        if (current == OrderStatus.SHIPPING && target == OrderStatus.COMPLETED) return true;
        // cho phép NEW -> SHIPPING (offline giao ngay)
        if (current == OrderStatus.NEW && target == OrderStatus.SHIPPING) return true;
        // cho phép PROCESSING -> COMPLETED (offline nhận hàng ngay)
        if (current == OrderStatus.PROCESSING && target == OrderStatus.COMPLETED) return true;
        // cho phép NEW -> COMPLETED
        if (current == OrderStatus.NEW && target == OrderStatus.COMPLETED) return true;
        return false;
    }

    // ====================== UPDATE PAYMENT ======================
    @Transactional
    public OrderResponse updatePayment(Long id, UpdatePaymentRequest request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(request.getPaymentStatus());

        // nếu refunded -> đảm bảo finalAmount có thể = 0 trong trường hợp hoàn hết
        if (request.getPaymentStatus() == PaymentStatus.REFUNDED
                && order.getFinalAmount() != null
                && order.getFinalAmount().compareTo(BigDecimal.ZERO) > 0) {
            // vẫn cho phép, nhưng không auto set final=0 vì còn “doanh thu thực” nếu chỉ đổi trạng thái.
        }

        return toDetailResponse(orderRepository.save(order));
    }

    // ====================== CANCEL ======================
    @Transactional
    public OrderResponse cancel(Long id, CancelOrderRequest request) {

        User currentUser = getCurrentUser();

        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            return toDetailResponse(order);
        }
        if (order.getOrderStatus() == OrderStatus.SHIPPING || order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy đơn khi đã giao/hoàn tất");
        }

        // hoàn kho
        for (OrderItem item : order.getItems()) {
            ProductVariant v = item.getVariant();
            if (v != null) {
                v.setStock(v.getStock() + item.getQuantity());
                v.setStatus("IN_STOCK");
                variantRepository.save(v);
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(request.getReason());
        order.setCancelledBy(currentUser);

        return toDetailResponse(orderRepository.save(order));
    }

    // ====================== RETURN ======================
    @Transactional
    public OrderResponse returnOrder(Long id, ReturnOrderRequest request) {

        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new RuntimeException("Chỉ cho phép trả hàng khi đơn đã COMPLETED");
        }

        if (request.getNote() != null) {
            order.setReturnNote(request.getNote());
        }

        BigDecimal totalReturnThisTime = BigDecimal.ZERO;

        for (ReturnOrderItemRequest r : request.getItems()) {

            OrderItem item = orderItemRepository.findById(r.getOrderItemId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy OrderItem id=" + r.getOrderItemId()));

            if (item.getOrder() == null || !Objects.equals(item.getOrder().getId(), order.getId())) {
                throw new RuntimeException("OrderItem không thuộc đơn hàng này");
            }

            int returnQty = Optional.ofNullable(r.getQuantity()).orElse(0);
            if (returnQty <= 0) throw new RuntimeException("Số lượng trả phải >= 1");

            int remaining = item.getQuantity() - item.getReturnedQuantity();
            if (returnQty > remaining) {
                throw new RuntimeException("Số lượng trả vượt quá số lượng còn lại. Còn lại: " + remaining);
            }

            // tính tiền trả theo giá thực thu bình quân
            BigDecimal perUnitPaid = safeDivide(item.getLineTotalAmount(), BigDecimal.valueOf(item.getQuantity()));
            BigDecimal returnAmount = perUnitPaid.multiply(BigDecimal.valueOf(returnQty)).setScale(2, RoundingMode.HALF_UP);

            item.setReturnedQuantity(item.getReturnedQuantity() + returnQty);
            item.setReturnNote(r.getNote());
            item.setReturnedAt(LocalDateTime.now());

            orderItemRepository.save(item);

            // hoàn kho
            ProductVariant v = item.getVariant();
            if (v != null) {
                v.setStock(v.getStock() + returnQty);
                v.setStatus("IN_STOCK");
                variantRepository.save(v);
            }

            totalReturnThisTime = totalReturnThisTime.add(returnAmount);
        }

        // update order return totals
        order.setReturnedAmount(nvl(order.getReturnedAmount()).add(totalReturnThisTime));
        BigDecimal finalAmount = nvl(order.getTotalAmount()).subtract(nvl(order.getReturnedAmount()));
        order.setFinalAmount(finalAmount.max(BigDecimal.ZERO));

        // update return status
        boolean allReturned = order.getItems().stream()
                .allMatch(i -> i.getReturnedQuantity() != null && i.getReturnedQuantity().intValue() >= i.getQuantity());

        boolean anyReturned = order.getItems().stream()
                .anyMatch(i -> i.getReturnedQuantity() != null && i.getReturnedQuantity() > 0);

        if (!anyReturned) {
            order.setReturnStatus(ReturnStatus.NONE);
        } else if (allReturned) {
            order.setReturnStatus(ReturnStatus.RETURNED);
            order.setReturnedAt(LocalDateTime.now());

            // nếu đã thanh toán và trả hết -> coi như refunded
            if (order.getPaymentStatus() == PaymentStatus.PAID
                    && order.getFinalAmount().compareTo(BigDecimal.ZERO) == 0) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        } else {
            order.setReturnStatus(ReturnStatus.PARTIALLY_RETURNED);
        }

        return toDetailResponse(orderRepository.save(order));
    }

    // ====================== SOFT DELETE ======================
    @Transactional
    public void softDelete(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        order.setDeleted(true);
        order.setDeletedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    // ====================== MAPPING ======================
    private OrderResponse toSummaryResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .customerId(o.getCustomer() != null ? o.getCustomer().getId() : null)
                .customerName(o.getCustomer() != null ? o.getCustomer().getTen() : null)
                .customerPhone(o.getCustomer() != null ? o.getCustomer().getSoDienThoai() : null)
                .customerEmail(o.getCustomer() != null ? o.getCustomer().getEmail() : null)
                .customerType(o.getCustomer() != null ? o.getCustomer().getLoaiKhach() : null)
                .createdById(o.getCreatedBy() != null ? o.getCreatedBy().getId() : null)
                .createdByUsername(o.getCreatedBy() != null ? o.getCreatedBy().getUsername() : null)
                .createdByFullName(o.getCreatedBy() != null ? o.getCreatedBy().getFullName() : null)
                .channel(o.getChannel())
                .orderStatus(o.getOrderStatus())
                .paymentStatus(o.getPaymentStatus())
                .paymentMethod(o.getPaymentMethod())
                .returnStatus(o.getReturnStatus())
                .subtotalAmount(o.getSubtotalAmount())
                .discountAmount(o.getDiscountAmount())
                .shippingFee(o.getShippingFee())
                .totalAmount(o.getTotalAmount())
                .returnedAmount(o.getReturnedAmount())
                .finalAmount(o.getFinalAmount())
                .currencyCode(o.getCurrencyCode())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .shippedAt(o.getShippedAt())
                .completedAt(o.getCompletedAt())
                .cancelledAt(o.getCancelledAt())
                .returnedAt(o.getReturnedAt())
                .cancelReason(o.getCancelReason())
                .cancelledById(o.getCancelledBy() != null ? o.getCancelledBy().getId() : null)
                .emailSent(o.getEmailSent())
                .emailSentAt(o.getEmailSentAt())
                .build();
    }

    private OrderResponse toDetailResponse(Order o) {
        List<OrderItemResponse> items = null;
        if (o.getItems() != null) {
            items = o.getItems().stream()
                    .map(i -> OrderItemResponse.builder()
                            .id(i.getId())
                            .variantId(i.getVariant() != null ? i.getVariant().getId() : null)
                            .sku(i.getVariant() != null ? i.getVariant().getSku() : null)
                            .productName(i.getVariant() != null && i.getVariant().getProduct() != null ? i.getVariant().getProduct().getName() : null)
                            .size(i.getVariant() != null ? i.getVariant().getSize() : null)
                            .colorway(i.getVariant() != null ? i.getVariant().getColorway() : null)
                            .skuSnapshot(i.getSkuSnapshot())
                            .productNameSnapshot(i.getProductNameSnapshot())
                            .unitPrice(i.getUnitPrice())
                            .quantity(i.getQuantity())
                            .lineDiscountAmount(i.getLineDiscountAmount())
                            .lineTotalAmount(i.getLineTotalAmount())
                            .returnedQuantity(i.getReturnedQuantity())
                            .returnNote(i.getReturnNote())
                            .returnedAt(i.getReturnedAt())
                            .build())
                    .collect(Collectors.toList());
        }

        return toSummaryResponse(o).toBuilder()
                .promotionCode(o.getPromotionCode())
                .promotionDiscountAmount(o.getPromotionDiscountAmount())
                .shippingCarrier(o.getShippingCarrier())
                .trackingCode(o.getTrackingCode())
                .note(o.getNote())
                .returnNote(o.getReturnNote())
                .items(items)
                .build();
    }

    // ====================== HELPERS ======================
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RuntimeException("Chưa đăng nhập");
        }
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user: " + username));
    }

    private String generateOrderCode() {
        // ORD-20260303-123045-4821
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(df);
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            int rand = 1000 + random.nextInt(9000);
            String code = "ORD-" + datePart + "-" + timePart + "-" + rand;
            if (!orderRepository.existsByOrderCode(code)) return code;
        }
        // fallback
        return "ORD-" + datePart + "-" + uuid4();
    }

    private String uuid4() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private BigDecimal safeDivide(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || b.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return a.divide(b, 6, RoundingMode.HALF_UP);
    }

    // ===== Promotion calculation for order line =====
    private PriceCalcResult applyBestPromotion(Long variantId, BigDecimal baseUnitPrice, int quantity, String loaiKhach) {

        BigDecimal originalTotal = baseUnitPrice.multiply(BigDecimal.valueOf(quantity));
        LocalDateTime now = LocalDateTime.now();

        List<Promotion> promotions = promotionRepository.findActivePromotions(variantId, now);

        // filter by customer group
        List<Promotion> eligible = promotions.stream()
                .filter(p -> {
                    String group = p.getCustomerGroup();
                    return group == null
                            || "ALL".equalsIgnoreCase(group)
                            || (loaiKhach != null && loaiKhach.equalsIgnoreCase(group));
                })
                .collect(Collectors.toList());

        if (eligible.isEmpty()) {
            return new PriceCalcResult(originalTotal, null);
        }

        BigDecimal bestFinal = null;
        Integer bestPriority = 0;
        Long bestId = null;
        Promotion bestPromo = null;

        for (Promotion p : eligible) {
            BigDecimal finalTotal = calculatePromotionTotal(p, baseUnitPrice, quantity, originalTotal);

            Integer prio = p.getPriority() != null ? p.getPriority() : 0;
            Long pid = p.getId();

            boolean isBetter = false;
            if (bestFinal == null) {
                isBetter = true;
            } else if (finalTotal.compareTo(bestFinal) < 0) {
                isBetter = true;
            } else if (finalTotal.compareTo(bestFinal) == 0 && prio > bestPriority) {
                isBetter = true;
            } else if (finalTotal.compareTo(bestFinal) == 0 && prio.equals(bestPriority) && bestId != null && pid != null && pid > bestId) {
                isBetter = true;
            }

            if (isBetter) {
                bestFinal = finalTotal;
                bestPriority = prio;
                bestId = pid;
                bestPromo = p;
            }
        }

        if (bestFinal == null) bestFinal = originalTotal;

        return new PriceCalcResult(bestFinal, bestPromo);
    }

    private BigDecimal calculatePromotionTotal(
            Promotion promotion,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal originalTotal
    ) {

        if (promotion.getDiscountType() == null) return originalTotal;

        switch (promotion.getDiscountType()) {

            case PERCENT:
                BigDecimal percent = promotion.getDiscountValue();
                if (percent == null) return originalTotal;
                if (percent.compareTo(BigDecimal.valueOf(100)) > 0) {
                    percent = BigDecimal.valueOf(100);
                }
                BigDecimal discount = originalTotal
                        .multiply(percent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                return originalTotal.subtract(discount).max(BigDecimal.ZERO);

            case AMOUNT:
                BigDecimal amount = promotion.getDiscountValue();
                if (amount == null) return originalTotal;
                return originalTotal.subtract(amount).max(BigDecimal.ZERO);

            case BUY_2_GET_1:
                int buy = 2;
                int get = 1;
                int groupSize = buy + get;
                int free = (quantity / groupSize) * get;
                int pay = quantity - free;
                return unitPrice.multiply(BigDecimal.valueOf(pay));

            default:
                return originalTotal;
        }
    }

    // small value object
    private static class PriceCalcResult {
        private final BigDecimal finalTotal;
        private final Promotion promotion;

        public PriceCalcResult(BigDecimal finalTotal, Promotion promotion) {
            this.finalTotal = finalTotal;
            this.promotion = promotion;
        }

        public BigDecimal getFinalTotal() {
            return finalTotal;
        }

        public Promotion getPromotion() {
            return promotion;
        }
    }
}
