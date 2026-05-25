package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.order.returning.*;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.entity.order.*;
import com.sneakershop.backend.entity.order.enums.*;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.login.UserRepository;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.repository.order.ReturnRequestRepository;
import com.sneakershop.backend.repository.product.ProductVariantRepository;
import com.sneakershop.backend.service.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnRefundService {

    private static final int RETURN_ALLOWED_DAYS = 7;
    private static final List<ReturnRequestStatus> ACTIVE_STATUSES = Arrays.asList(
            ReturnRequestStatus.REQUESTED,
            ReturnRequestStatus.RECEIVED,
            ReturnRequestStatus.ACCEPTED,
            ReturnRequestStatus.PENDING,
            ReturnRequestStatus.APPROVED
    );
    private static final List<ReturnRequestStatus> COUNTED_RETURN_STATUSES = Arrays.asList(
            ReturnRequestStatus.REQUESTED,
            ReturnRequestStatus.RECEIVED,
            ReturnRequestStatus.ACCEPTED,
            ReturnRequestStatus.COMPLETED,
            ReturnRequestStatus.PENDING,
            ReturnRequestStatus.APPROVED,
            ReturnRequestStatus.REFUNDED
    );

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final CustomerService customerService;
    private final OrderInventoryService orderInventoryService;

    @Transactional(readOnly = true)
    public List<ReturnRefundResponse> listAdmin(ReturnRequestStatus status) {
        List<ReturnRequest> requests = status == null
                ? returnRequestRepository.findAllByOrderByCreatedAtDesc()
                : returnRequestRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return requests.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnRefundResponse> listMine() {
        Customer customer = currentCustomerOrThrow();
        return returnRequestRepository.findAllByCustomer_IdOrderByCreatedAtDesc(customer.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReturnRefundResponse detailAdmin(Long id) {
        return toResponse(getRequestOr404(id));
    }

    @Transactional(readOnly = true)
    public ReturnRefundResponse detailMine(Long id) {
        Customer customer = currentCustomerOrThrow();
        ReturnRequest request = getRequestOr404(id);
        if (request.getCustomer() == null || !Objects.equals(request.getCustomer().getId(), customer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem yêu cầu trả hàng này.");
        }
        return toResponse(request);
    }

    @Transactional
    public ReturnRefundResponse createByCustomer(CreateReturnRefundRequest request) {
        Customer customer = currentCustomerOrThrow();
        Order order = getOrderOr404(request.getOrderId());
        if (order.getCustomer() == null || !Objects.equals(order.getCustomer().getId(), customer.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng không thuộc tài khoản hiện tại.");
        }
        return toResponse(createRequest(order, customer, request, "CUSTOMER"));
    }

    @Transactional
    public ReturnRefundResponse createByAdmin(CreateReturnRefundRequest request) {
        Order order = getOrderOr404(request.getOrderId());
        Customer customer = order.getCustomer();
        return toResponse(createRequest(order, customer, request, currentActor()));
    }

    @Transactional
    public ReturnRefundResponse approve(Long id, AdminApproveReturnRequest request) {
        ReturnRequest rr = getRequestOr404(id);
        requireAnyStatus(rr, ReturnRequestStatus.RECEIVED, ReturnRequestStatus.APPROVED);
        String note = blankToNull(request == null ? null : request.getAdminNote());
        validateReceivedQuantities(rr);
        rr.setAdminNote(note);
        rr.setApprovedAt(LocalDateTime.now());
        changeStatus(rr, ReturnRequestStatus.ACCEPTED, note == null ? "Đã duyệt hàng hoàn trả." : note);
        return toResponse(returnRequestRepository.save(rr));
    }

    @Transactional
    public ReturnRefundResponse reject(Long id, AdminRejectReturnRequest request) {
        ReturnRequest rr = getRequestOr404(id);
        requireAnyStatus(rr, ReturnRequestStatus.REQUESTED, ReturnRequestStatus.RECEIVED, ReturnRequestStatus.PENDING, ReturnRequestStatus.APPROVED);
        String reason = blankToNull(request == null ? null : request.getReason());
        if (reason == null) reason = "Admin từ chối yêu cầu trả hàng.";
        rr.setRejectReason(reason);
        rr.setRejectedAt(LocalDateTime.now());
        changeStatus(rr, ReturnRequestStatus.REJECTED, reason);
        return toResponse(returnRequestRepository.save(rr));
    }

    @Transactional
    public ReturnRefundResponse receive(Long id, AdminReceiveReturnRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập thông tin hàng hoàn đã nhận.");
        }
        ReturnRequest rr = getRequestOr404(id);
        requireAnyStatus(rr, ReturnRequestStatus.REQUESTED, ReturnRequestStatus.PENDING);
        Map<Long, ReturnRequestItem> currentItems = rr.getItems().stream()
                .collect(Collectors.toMap(ReturnRequestItem::getId, x -> x));
        Set<Long> seen = new HashSet<>();
        for (AdminReceiveReturnItemRequest itemReq : safeList(request.getItems())) {
            ReturnRequestItem item = currentItems.get(itemReq.getReturnItemId());
            if (item == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm trả hàng không thuộc yêu cầu này.");
            }
            if (!seen.add(item.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm trả hàng bị trùng trong yêu cầu.");
            }
            applyReceiveInspection(item, itemReq);
        }
        rr.setAdminNote(blankToNull(request.getAdminNote()));
        rr.setReceivedAt(LocalDateTime.now());
        changeStatus(rr, ReturnRequestStatus.RECEIVED, blankToNull(request.getAdminNote()) == null ? "Đã xác nhận nhận hàng hoàn." : blankToNull(request.getAdminNote()));
        return toResponse(returnRequestRepository.save(rr));
    }

    @Transactional
    public ReturnRefundResponse refund(Long id, AdminRefundReturnRequest request) {
        ReturnRequest rr = getRequestOr404(id);
        requireAnyStatus(rr, ReturnRequestStatus.ACCEPTED, ReturnRequestStatus.REFUNDED);
        BigDecimal expectedRefund = calculateReceivedRefundAmount(rr);
        if (expectedRefund.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền hoàn không hợp lệ.");
        }
        BigDecimal refundAmount = request != null && request.getRefundAmount() != null ? request.getRefundAmount() : expectedRefund;
        if (refundAmount.signum() < 0 || refundAmount.compareTo(expectedRefund) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền hoàn không hợp lệ.");
        }
        rr.setRefundAmount(refundAmount);
        rr.setRefundMethod(request == null || request.getRefundMethod() == null ? RefundMethod.BANK_TRANSFER : request.getRefundMethod());
        rr.setRefundTransactionCode(blankToNull(request == null ? null : request.getTransactionCode()));
        rr.setAdminNote(blankToNull(request == null ? null : request.getAdminNote()));
        rr.setRefundedAt(LocalDateTime.now());
        restockReceivedItems(rr);
        applyReturnedQuantities(rr);
        updateOrderReturnSummary(rr.getOrder(), refundAmount);
        subtractVipPoints(rr);
        rr.setCompletedAt(LocalDateTime.now());
        changeStatus(rr, ReturnRequestStatus.COMPLETED, blankToNull(request == null ? null : request.getAdminNote()) == null ? "Đã hoàn tiền và hoàn tất đơn hoàn trả." : blankToNull(request.getAdminNote()));
        return toResponse(returnRequestRepository.save(rr));
    }

    @Transactional
    public ReturnRefundResponse complete(Long id) {
        ReturnRequest rr = getRequestOr404(id);
        requireAnyStatus(rr, ReturnRequestStatus.ACCEPTED, ReturnRequestStatus.REFUNDED);
        return refund(id, null);
    }

    private ReturnRequest createRequest(Order order, Customer customer, CreateReturnRefundRequest req, String actor) {
        validateReturnableOrder(order);
        if (returnRequestRepository.existsByOrder_IdAndStatusIn(order.getId(), ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu trả hàng đã tồn tại hoặc đang được xử lý.");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn sản phẩm cần trả hàng.");
        }

        Map<Long, Integer> requestedQtyByOrderItemId = new LinkedHashMap<>();
        Map<Long, String> noteByOrderItemId = new HashMap<>();
        for (CreateReturnRefundItemRequest item : req.getItems()) {
            if (item.getOrderItemId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng trả phải là số nguyên dương.");
            }
            requestedQtyByOrderItemId.merge(item.getOrderItemId(), item.getQuantity(), Integer::sum);
            noteByOrderItemId.put(item.getOrderItemId(), item.getNote());
        }

        Map<Long, OrderItem> orderItems = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, x -> x));

        ReturnRequest rr = new ReturnRequest();
        rr.setCode(generateCode());
        rr.setOrder(order);
        rr.setCustomer(customer);
        rr.setReason(blankToNull(req.getReason()));
        rr.setCustomerNote(blankToNull(req.getCustomerNote()));
        rr.setAdminNote(blankToNull(req.getAdminNote()));
        rr.setStatus(ReturnRequestStatus.REQUESTED);

        BigDecimal totalRefund = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : requestedQtyByOrderItemId.entrySet()) {
            OrderItem orderItem = orderItems.get(entry.getKey());
            if (orderItem == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm trả hàng không thuộc đơn hàng này.");
            }
            int requestedQty = entry.getValue();
            int boughtQty = nz(orderItem.getQuantity());
            int alreadyReturned = returnedQuantityBefore(orderItem.getId());
            int available = boughtQty - alreadyReturned;
            if (available <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm này đã được trả hết số lượng cho phép.");
            }
            if (requestedQty > boughtQty || requestedQty > available) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng trả không được vượt quá số lượng đã mua.");
            }

            BigDecimal unitPrice = resolveRefundUnitPrice(orderItem);
            BigDecimal lineRefund = unitPrice.multiply(BigDecimal.valueOf(requestedQty));
            ReturnRequestItem rri = new ReturnRequestItem();
            rri.setReturnRequest(rr);
            rri.setOrderItem(orderItem);
            rri.setVariant(orderItem.getVariant());
            rri.setQuantity(requestedQty);
            rri.setUnitPrice(unitPrice);
            rri.setRefundAmount(lineRefund);
            rri.setNote(blankToNull(noteByOrderItemId.get(orderItem.getId())));
            rr.getItems().add(rri);
            totalRefund = totalRefund.add(lineRefund);
        }
        BigDecimal remainingRefundable = calculateRemainingRefundableProductAmount(order);
        if (totalRefund.signum() <= 0 || totalRefund.compareTo(remainingRefundable) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền hoàn không hợp lệ.");
        }
        rr.setRefundAmount(totalRefund);
        addHistory(rr, null, ReturnRequestStatus.REQUESTED, "Tạo đơn hoàn trả từ chi tiết đơn hàng.", actor);
        return returnRequestRepository.save(rr);
    }

    private void validateReturnableOrder(Order order) {
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại.");
        }
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn hàng đã hoàn thành mới được yêu cầu trả hàng.");
        }
        LocalDateTime completedAt = order.getCompletedAt();
        if (completedAt == null) completedAt = order.getDeliveredAt();
        if (completedAt == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ đơn hàng đã hoàn thành mới được yêu cầu trả hàng.");
        }
        if (completedAt.plusDays(RETURN_ALLOWED_DAYS).isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng đã quá thời hạn trả hàng.");
        }
    }

    private void restockReceivedItems(ReturnRequest rr) {
        for (ReturnRequestItem item : rr.getItems()) {
            int restockQty = nz(item.getRestockQuantity());
            if (restockQty <= 0) continue;
            ProductVariant variant = item.getVariant();
            if (variant == null && item.getOrderItem() != null) variant = item.getOrderItem().getVariant();
            if (variant == null || variant.getId() == null) continue;
            orderInventoryService.restockReturned(
                    variant.getId(),
                    restockQty,
                    rr.getId(),
                    "Nhập lại kho từ đơn hoàn trả " + (rr.getCode() == null ? "" : rr.getCode())
            );
        }
    }

    private void applyReturnedQuantities(ReturnRequest rr) {
        for (ReturnRequestItem item : rr.getItems()) {
            OrderItem orderItem = item.getOrderItem();
            int completedQty = nz(item.getReceivedQuantity());
            if (completedQty <= 0) continue;
            int newReturned = Math.min(nz(orderItem.getQuantity()), nz(orderItem.getReturnedQuantity()) + completedQty);
            orderItem.setReturnedQuantity(newReturned);
            orderItem.setReturnedAt(LocalDateTime.now());
            orderItem.setReturnNote(item.getNote());
        }
    }

    private void updateOrderReturnSummary(Order order, BigDecimal refundAmount) {
        BigDecimal oldReturnedAmount = nz(order.getReturnedAmount());
        order.setReturnedAmount(oldReturnedAmount.add(nz(refundAmount)));
        order.setReturnedAt(LocalDateTime.now());
        order.setReturnStatus(isAllItemsReturned(order) ? ReturnStatus.RETURNED : ReturnStatus.PARTIALLY_RETURNED);
        order.setOrderStatus(isAllItemsReturned(order) ? OrderStatus.RETURNED : OrderStatus.PARTIALLY_RETURNED);
        if (order.getReturnedAmount().compareTo(nz(order.getFinalAmount())) >= 0 || isAllItemsReturned(order)) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        } else {
            order.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }
        orderRepository.save(order);
    }

    private void subtractVipPoints(ReturnRequest rr) {
        Order order = rr.getOrder();
        if (order != null && order.getCustomer() != null && nz(rr.getRefundAmount()).signum() > 0) {
            customerService.subtractPointsFromReturn(order.getCustomer().getId(), rr.getRefundAmount(), rr.getCode());
        }
    }

    private boolean isAllItemsReturned(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) return false;
        for (OrderItem item : order.getItems()) {
            if (nz(item.getReturnedQuantity()) < nz(item.getQuantity())) return false;
        }
        return true;
    }

    private BigDecimal calculateReceivedRefundAmount(ReturnRequest rr) {
        BigDecimal total = BigDecimal.ZERO;
        for (ReturnRequestItem item : rr.getItems()) {
            int receivedQty = nz(item.getReceivedQuantity());
            if (receivedQty <= 0) continue;
            if (receivedQty > nz(item.getQuantity())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng nhận hàng trả không hợp lệ.");
            }
            if (item.getInspections() != null && !item.getInspections().isEmpty()) {
                total = total.add(nz(item.getRefundAmount()));
            } else {
                total = total.add(nz(item.getUnitPrice()).multiply(BigDecimal.valueOf(receivedQty)));
            }
        }
        return total;
    }

    private BigDecimal resolveRefundUnitPrice(OrderItem item) {
        int qty = Math.max(1, nz(item.getQuantity()));
        BigDecimal itemLineTotal = resolveItemLineTotal(item);
        Order order = item.getOrder();

        if (order != null) {
            BigDecimal orderItemsTotal = calculateOrderItemsRefundBase(order);
            BigDecimal paidProductAmount = calculateOrderPaidProductAmount(order);

            // Với đơn có voucher/VIP/manual discount cấp đơn, lineTotal của item có thể lớn hơn số tiền khách thực trả.
            // Vì vậy tiền hoàn phải phân bổ theo tỷ lệ giá trị item trên tổng tiền hàng, không lấy giá hiện tại hoặc line gross rồi so thẳng với finalAmount.
            if (orderItemsTotal.signum() > 0 && paidProductAmount.signum() > 0) {
                BigDecimal proratedLineRefund = itemLineTotal
                        .multiply(paidProductAmount)
                        .divide(orderItemsTotal, 2, RoundingMode.HALF_UP);
                if (proratedLineRefund.signum() > 0) {
                    return proratedLineRefund.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
                }
            }
        }

        if (itemLineTotal.signum() > 0) {
            return itemLineTotal.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
        }
        return nz(item.getUnitPrice());
    }

    private BigDecimal resolveItemLineTotal(OrderItem item) {
        BigDecimal lineTotal = nz(item.getLineTotalAmount());
        if (lineTotal.signum() > 0) return lineTotal;
        BigDecimal unitPrice = nz(item.getUnitPrice());
        int qty = Math.max(1, nz(item.getQuantity()));
        return unitPrice.multiply(BigDecimal.valueOf(qty));
    }

    private BigDecimal calculateOrderItemsRefundBase(Order order) {
        if (order == null || order.getItems() == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            total = total.add(resolveItemLineTotal(item));
        }
        return total;
    }

    private BigDecimal calculateOrderPaidProductAmount(Order order) {
        BigDecimal finalAmount = nz(order.getFinalAmount());
        if (finalAmount.signum() <= 0) return BigDecimal.ZERO;

        BigDecimal shippingFee = nz(order.getShippingFee());
        BigDecimal shippingDiscount = nz(order.getShippingDiscountAmount());
        BigDecimal netShippingFee = shippingFee.subtract(shippingDiscount);
        if (netShippingFee.signum() < 0) netShippingFee = BigDecimal.ZERO;

        BigDecimal paidProductAmount = finalAmount.subtract(netShippingFee);
        if (paidProductAmount.signum() < 0) paidProductAmount = BigDecimal.ZERO;
        return paidProductAmount;
    }

    private BigDecimal calculateRemainingRefundableProductAmount(Order order) {
        BigDecimal remaining = calculateOrderPaidProductAmount(order).subtract(nz(order.getReturnedAmount()));
        return remaining.signum() < 0 ? BigDecimal.ZERO : remaining;
    }

    private int returnedQuantityBefore(Long orderItemId) {
        Long total = returnRequestRepository.sumReturnedQuantityByOrderItemIdAndStatuses(orderItemId, COUNTED_RETURN_STATUSES);
        return total == null ? 0 : total.intValue();
    }

    private ReturnRequest getRequestOr404(Long id) {
        return returnRequestRepository.findByIdAndOrder_DeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Yêu cầu trả hàng không tồn tại."));
    }

    private Order getOrderOr404(Long id) {
        return orderRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại."));
    }

    private void requireStatus(ReturnRequest rr, ReturnRequestStatus expected) {
        if (rr.getStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái yêu cầu trả hàng không hợp lệ.");
        }
    }

    private void requireAnyStatus(ReturnRequest rr, ReturnRequestStatus... expectedStatuses) {
        for (ReturnRequestStatus expected : expectedStatuses) {
            if (rr.getStatus() == expected) return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái yêu cầu trả hàng không hợp lệ.");
    }

    private void validateReceivedQuantities(ReturnRequest rr) {
        int totalReceived = 0;
        for (ReturnRequestItem item : rr.getItems()) {
            int receivedQty = nz(item.getReceivedQuantity());
            totalReceived += receivedQty;
            if (receivedQty < 0 || receivedQty > nz(item.getQuantity())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng nhận hàng trả không hợp lệ.");
            }
            if (nz(item.getRestockQuantity()) < 0 || nz(item.getRestockQuantity()) > receivedQty) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng nhập lại kho không hợp lệ.");
            }
            validateInspectionSummary(item);
        }
        if (totalReceived <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phải có ít nhất 1 sản phẩm được shop nhận lại.");
        }
    }

    private void applyReceiveInspection(ReturnRequestItem item, AdminReceiveReturnItemRequest itemReq) {
        List<AdminReceiveReturnInspectionRequest> inspectionRequests = itemReq.getInspections();
        if (inspectionRequests != null && !inspectionRequests.isEmpty()) {
            item.getInspections().clear();
            int totalReceived = 0;
            int totalRestock = 0;
            BigDecimal totalRefund = BigDecimal.ZERO;
            ReturnConditionStatus firstStatus = null;
            boolean mixed = false;

            for (AdminReceiveReturnInspectionRequest inspectionReq : inspectionRequests) {
                int qty = nz(inspectionReq.getQuantity());
                int restock = nz(inspectionReq.getRestockQuantity());
                int refundQty = nz(inspectionReq.getRefundQuantity());
                BigDecimal refundRate = nz(inspectionReq.getRefundRate());

                if (qty < 0 || restock < 0 || refundQty < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng phân loại hàng hoàn không hợp lệ.");
                }
                if (restock > qty) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng nhập kho không được vượt quá số lượng của dòng phân loại.");
                }
                if (refundQty > qty) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng được hoàn tiền không được vượt quá số lượng của dòng phân loại.");
                }
                if (refundRate.signum() < 0 || refundRate.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tỷ lệ hoàn tiền phải nằm trong khoảng 0% đến 100%.");
                }
                ReturnConditionStatus condition = inspectionReq.getConditionStatus() == null ? ReturnConditionStatus.NEW : inspectionReq.getConditionStatus();
                if (condition != ReturnConditionStatus.NEW && restock > 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hàng còn mới/bán lại được mới được nhập lại kho bán.");
                }

                BigDecimal lineRefund = nz(item.getUnitPrice())
                        .multiply(BigDecimal.valueOf(refundQty))
                        .multiply(refundRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                ReturnRequestItemInspection inspection = new ReturnRequestItemInspection();
                inspection.setReturnItem(item);
                inspection.setConditionStatus(condition);
                inspection.setQuantity(qty);
                inspection.setRestockQuantity(restock);
                inspection.setRefundQuantity(refundQty);
                inspection.setRefundRate(refundRate.setScale(2, RoundingMode.HALF_UP));
                inspection.setRefundAmount(lineRefund);
                inspection.setResponsibility(blankToNull(inspectionReq.getResponsibility()));
                inspection.setNote(blankToNull(inspectionReq.getNote()));
                item.getInspections().add(inspection);

                totalReceived += qty;
                totalRestock += restock;
                totalRefund = totalRefund.add(lineRefund);
                if (firstStatus == null) firstStatus = condition;
                else if (firstStatus != condition) mixed = true;
            }

            if (totalReceived < 0 || totalReceived > nz(item.getQuantity())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tổng số lượng phân loại không được vượt quá số lượng yêu cầu trả.");
            }
            item.setReceivedQuantity(totalReceived);
            item.setRestockQuantity(totalRestock);
            item.setRefundAmount(totalRefund);
            item.setConditionStatus(firstStatus == null ? itemReq.getConditionStatus() : (mixed ? ReturnConditionStatus.NOT_RESELLABLE : firstStatus));
            item.setNote(blankToNull(itemReq.getNote()));
            return;
        }

        int received = nz(itemReq.getReceivedQuantity());
        int restock = nz(itemReq.getRestockQuantity());
        if (received < 0 || received > nz(item.getQuantity())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng nhận hàng trả không hợp lệ.");
        }
        if (restock < 0 || restock > received) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng nhập lại kho không hợp lệ.");
        }
        ReturnConditionStatus condition = itemReq.getConditionStatus() == null ? ReturnConditionStatus.NEW : itemReq.getConditionStatus();
        if (condition != ReturnConditionStatus.NEW && restock > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ hàng còn mới/bán lại được mới được nhập lại kho bán.");
        }
        item.getInspections().clear();
        item.setReceivedQuantity(received);
        item.setRestockQuantity(restock);
        item.setConditionStatus(condition);
        item.setRefundAmount(nz(item.getUnitPrice()).multiply(BigDecimal.valueOf(received)));
        item.setNote(blankToNull(itemReq.getNote()));
    }

    private void validateInspectionSummary(ReturnRequestItem item) {
        if (item.getInspections() == null || item.getInspections().isEmpty()) return;
        int totalQty = 0;
        int totalRestock = 0;
        int totalRefundQty = 0;
        BigDecimal totalRefund = BigDecimal.ZERO;
        for (ReturnRequestItemInspection inspection : item.getInspections()) {
            int qty = nz(inspection.getQuantity());
            int restock = nz(inspection.getRestockQuantity());
            int refundQty = nz(inspection.getRefundQuantity());
            if (qty < 0 || restock < 0 || refundQty < 0 || restock > qty || refundQty > qty) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi tiết phân loại hàng hoàn không hợp lệ.");
            }
            totalQty += qty;
            totalRestock += restock;
            totalRefundQty += refundQty;
            totalRefund = totalRefund.add(nz(inspection.getRefundAmount()));
        }
        if (totalQty != nz(item.getReceivedQuantity())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tổng số lượng phân loại phải bằng số lượng shop đã nhận.");
        }
        if (totalRestock != nz(item.getRestockQuantity()) || totalRestock > nz(item.getReceivedQuantity())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tổng số lượng nhập kho không hợp lệ.");
        }
        if (totalRefundQty > nz(item.getReceivedQuantity())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tổng số lượng được hoàn tiền không hợp lệ.");
        }
        item.setRefundAmount(totalRefund);
    }

    private void changeStatus(ReturnRequest rr, ReturnRequestStatus next, String note) {
        ReturnRequestStatus old = rr.getStatus();
        rr.setStatus(next);
        addHistory(rr, old, next, note, currentActor());
    }

    private void addHistory(ReturnRequest rr, ReturnRequestStatus oldStatus, ReturnRequestStatus newStatus, String note, String actor) {
        ReturnRequestHistory history = new ReturnRequestHistory();
        history.setReturnRequest(rr);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setNote(note);
        history.setCreatedBy(actor);
        rr.getHistories().add(history);
    }

    private Customer currentCustomerOrThrow() {
        User user = currentUserOrThrow();
        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản hiện tại chưa liên kết khách hàng.");
        }
        return customer;
    }

    private User currentUserOrThrow() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập.");
        }
        String key = auth.getName();
        return userRepository.findByUsername(key)
                .or(() -> userRepository.findByEmail(key))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập."));
    }

    private String currentActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth.getName() == null ? "SYSTEM" : auth.getName();
    }

    private String generateCode() {
        String code;
        do {
            code = "RT" + System.currentTimeMillis() + (int) (Math.random() * 1000);
        } while (returnRequestRepository.existsByCode(code));
        return code;
    }

    private ReturnRefundResponse toResponse(ReturnRequest rr) {
        ReturnRefundResponse dto = new ReturnRefundResponse();
        dto.setId(rr.getId());
        dto.setCode(rr.getCode());
        dto.setOrderId(rr.getOrder() != null ? rr.getOrder().getId() : null);
        dto.setOrderCode(rr.getOrder() != null ? rr.getOrder().getOrderCode() : null);
        dto.setCustomerId(rr.getCustomer() != null ? rr.getCustomer().getId() : null);
        dto.setCustomerName(rr.getCustomer() != null ? rr.getCustomer().getTen() : null);
        dto.setCustomerPhone(rr.getCustomer() != null ? rr.getCustomer().getPhone() : null);
        dto.setStatus(rr.getStatus());
        dto.setReason(rr.getReason());
        dto.setCustomerNote(rr.getCustomerNote());
        dto.setAdminNote(rr.getAdminNote());
        dto.setRejectReason(rr.getRejectReason());
        dto.setRefundAmount(rr.getRefundAmount());
        dto.setRefundMethod(rr.getRefundMethod());
        dto.setRefundTransactionCode(rr.getRefundTransactionCode());
        dto.setCreatedAt(rr.getCreatedAt());
        dto.setApprovedAt(rr.getApprovedAt());
        dto.setRejectedAt(rr.getRejectedAt());
        dto.setReceivedAt(rr.getReceivedAt());
        dto.setRefundedAt(rr.getRefundedAt());
        dto.setCompletedAt(rr.getCompletedAt());
        dto.setItems(rr.getItems() == null ? Collections.emptyList() : rr.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()));
        dto.setHistories(rr.getHistories() == null ? Collections.emptyList() : rr.getHistories().stream().map(this::toHistoryResponse).collect(Collectors.toList()));
        return dto;
    }

    private ReturnRefundItemResponse toItemResponse(ReturnRequestItem item) {
        OrderItem oi = item.getOrderItem();
        ReturnRefundItemResponse dto = new ReturnRefundItemResponse();
        dto.setId(item.getId());
        dto.setOrderItemId(oi != null ? oi.getId() : null);
        dto.setVariantId(item.getVariant() != null ? item.getVariant().getId() : null);
        dto.setProductName(oi != null ? oi.getProductNameSnapshot() : null);
        dto.setSku(oi != null ? oi.getSkuSnapshot() : null);
        dto.setColor(oi != null ? oi.getColorSnapshot() : null);
        dto.setSize(oi != null ? oi.getSizeSnapshot() : null);
        dto.setBoughtQuantity(oi != null ? oi.getQuantity() : null);
        dto.setPreviouslyReturnedQuantity(oi != null ? oi.getReturnedQuantity() : null);
        dto.setQuantity(item.getQuantity());
        dto.setReceivedQuantity(item.getReceivedQuantity());
        dto.setRestockQuantity(item.getRestockQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setRefundAmount(item.getRefundAmount());
        dto.setConditionStatus(item.getConditionStatus());
        dto.setNote(item.getNote());
        dto.setInspections(item.getInspections() == null ? Collections.emptyList() : item.getInspections().stream().map(this::toInspectionResponse).collect(Collectors.toList()));
        return dto;
    }

    private ReturnRefundInspectionResponse toInspectionResponse(ReturnRequestItemInspection inspection) {
        ReturnRefundInspectionResponse dto = new ReturnRefundInspectionResponse();
        dto.setId(inspection.getId());
        dto.setConditionStatus(inspection.getConditionStatus());
        dto.setQuantity(inspection.getQuantity());
        dto.setRestockQuantity(inspection.getRestockQuantity());
        dto.setRefundQuantity(inspection.getRefundQuantity());
        dto.setRefundRate(inspection.getRefundRate());
        dto.setRefundAmount(inspection.getRefundAmount());
        dto.setResponsibility(inspection.getResponsibility());
        dto.setNote(inspection.getNote());
        return dto;
    }

    private ReturnRefundHistoryResponse toHistoryResponse(ReturnRequestHistory history) {
        ReturnRefundHistoryResponse dto = new ReturnRefundHistoryResponse();
        dto.setOldStatus(history.getOldStatus());
        dto.setNewStatus(history.getNewStatus());
        dto.setNote(history.getNote());
        dto.setCreatedBy(history.getCreatedBy());
        dto.setCreatedAt(history.getCreatedAt());
        return dto;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private int nz(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }
}
