package com.sneakershop.backend.service.order;

import com.sneakershop.backend.dto.customer.CustomerSpendingDTO;
import com.sneakershop.backend.dto.customer.InactiveCustomerDTO;
import com.sneakershop.backend.audit.AuditAction; // Đã thêm import này
import com.sneakershop.backend.dto.order.*;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.entity.order.Order;
import com.sneakershop.backend.entity.order.OrderItem;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import com.sneakershop.backend.entity.product.ProductVariant;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.order.OrderRepository;
import com.sneakershop.backend.service.pricing.PricingCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final EntityManager em;
    private final CustomerRepository customerRepo;
    private final PricingCalculationService pricingCalculationService;

    private Order getOrderOr404(Long id) {
        return orderRepo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private void assertEditable(Order order) {
        if (order.getOrderStatus() != OrderStatus.NEW && order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order is not editable in status: " + order.getOrderStatus());
        }
    }

    private void assertCancelable(Order order) {
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed order cannot be cancelled.");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order already cancelled.");
        }
    }

    private void assertReturnable(Order order) {
        if (order.getOrderStatus() != OrderStatus.SHIPPING && order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Order is not returnable in status: " + order.getOrderStatus());
        }
    }

    // Checklist: cập nhật tồn kho khi hủy/hoàn trả (best-effort, không phụ thuộc module product)
    private void adjustVariantStockBestEffort(Long variantId, int delta) {
        if (variantId == null || delta == 0) return;
        String[] columns = {"stock", "quantity", "stock_quantity", "stockQuantity"};
        for (String col : columns) {
            try {
                int updated = em.createNativeQuery(
                        "UPDATE product_variant SET " + col + " = " + col + " + :delta WHERE id = :id")
                        .setParameter("delta", delta)
                        .setParameter("id", variantId)
                        .executeUpdate();
                if (updated > 0) return;
            } catch (Exception ignore) {
                // try next column
            }
        }
        // If cannot update (different schema), ignore to keep project stable.
    }

    @AuditAction(module = "ORDER", action = "CREATE", entity = "Order", description = "Tạo mới đơn hàng qua kênh: #{#req.channel}")
    @Transactional
    public OrderDetailDTO create(CreateOrderRequest req) {
        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setChannel(req.getChannel());
        order.setPaymentMethod(req.getPaymentMethod());
        // ===== LẤY LOẠI KHÁCH =====
        String customerType = "THUONG";

        if (req.getCustomerId() != null) {

            Customer customer = em.find(Customer.class, req.getCustomerId());

            if (customer != null && customer.getLoaiKhach() != null) {
                customerType = customer.getLoaiKhach();
            }
        }
        order.setNote(req.getNote());

        if (req.getShippingFee() != null) order.setShippingFee(nz(req.getShippingFee()));
        if (req.getDiscountAmount() != null) order.setDiscountAmount(nz(req.getDiscountAmount()));

        if (req.getCustomerId() != null) {
            order.setCustomer(em.getReference(Customer.class, req.getCustomerId()));
        }
        if (req.getCreatedById() != null) {
            order.setCreatedBy(em.getReference(User.class, req.getCreatedById()));
        }

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemCreateRequest ir : req.getItems()) {

            OrderItem it = new OrderItem();

            it.setOrder(order);
            it.setVariant(em.getReference(ProductVariant.class, ir.getVariantId()));
            it.setQuantity(ir.getQuantity());

            // ===== TÍNH GIÁ THEO KHÁCH + PROMOTION =====
            BigDecimal finalPrice =
                    pricingCalculationService.calculateFinalPrice(
                            ir.getVariantId(),
                            customerType
                    );

            it.setUnitPrice(finalPrice);

            if (ir.getLineDiscountAmount() != null)
                it.setLineDiscountAmount(nz(ir.getLineDiscountAmount()));

            it.setSkuSnapshot(ir.getSkuSnapshot());
            it.setProductNameSnapshot(ir.getProductNameSnapshot());

            calcLine(it);
            items.add(it);
        }
        order.setItems(items);

        recalcOrderTotals(order);
        Order saved = orderRepo.save(order);
        return toDetailDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> list(Optional<OrderStatus> statusOpt) {
        List<Order> orders = statusOpt
                .map(s -> orderRepo.findAllByOrderStatusAndDeletedFalseOrderByCreatedAtDesc(s))
                .orElseGet(orderRepo::findAllByDeletedFalseOrderByCreatedAtDesc);

        return orders.stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    // Checklist: hiển thị đơn hàng theo khách hàng
    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> listByCustomer(Long customerId) {
        return orderRepo.findAllByCustomer_IdAndDeletedFalseOrderByCreatedAtDesc(customerId)
                .stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    // Checklist: hiển thị đơn hàng theo nhân viên bán
    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> listByStaff(Long createdById) {
        return orderRepo.findAllByCreatedBy_IdAndDeletedFalseOrderByCreatedAtDesc(createdById)
                .stream().map(this::toSummaryDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDetailDTO detail(Long id) {
        Order order = getOrderOr404(id);
        return toDetailDTO(order);
    }

    @AuditAction(module = "ORDER", action = "UPDATE", entity = "Order", description = "Cập nhật thông tin đơn hàng ID: #{#id}")
    @Transactional
    public OrderDetailDTO update(Long id, UpdateOrderRequest req) {
        Order order = getOrderOr404(id);
        assertEditable(order);

        if (req.getChannel() != null) order.setChannel(req.getChannel());
        if (req.getPaymentMethod() != null) order.setPaymentMethod(req.getPaymentMethod());
        if (req.getPaymentStatus() != null) order.setPaymentStatus(req.getPaymentStatus());

        if (req.getShippingFee() != null) order.setShippingFee(nz(req.getShippingFee()));
        if (req.getDiscountAmount() != null) order.setDiscountAmount(nz(req.getDiscountAmount()));
        if (req.getNote() != null) order.setNote(req.getNote());

        recalcOrderTotals(order);
        return toDetailDTO(orderRepo.save(order));
    }

    @AuditAction(module = "ORDER", action = "CANCEL", entity = "Order", description = "Hủy đơn hàng ID: #{#id} với lý do: #{#req.reason}")
    @Transactional
    public OrderDetailDTO cancel(Long id, CancelOrderRequest req) {
        Order order = getOrderOr404(id);
        assertCancelable(order);

        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelReason(req.getReason());
        order.setCancelledAt(LocalDateTime.now());
        if (req.getCancelledById() != null) {
            order.setCancelledBy(em.getReference(User.class, req.getCancelledById()));
        }

        // Checklist: cập nhật tồn kho khi hủy đơn (cộng lại tồn)
        if (order.getItems() != null) {
            for (OrderItem it : order.getItems()) {
                if (it.getQuantity() != null && it.getQuantity() > 0) {
                    adjustVariantStockBestEffort(it.getVariantId(), it.getQuantity());
                }
            }
        }
        return toDetailDTO(orderRepo.save(order));
    }

    // Checklist: thêm trạng thái "Đang giao" + "Hoàn tất giao hàng"
    @AuditAction(module = "ORDER", action = "UPDATE_STATUS", entity = "Order", description = "Cập nhật trạng thái đơn hàng ID: #{#id} thành: #{#req.status}")
    @Transactional
    public OrderDetailDTO updateStatus(Long id, UpdateOrderStatusRequest req) {
        Order order = getOrderOr404(id);

        OrderStatus next = req.getStatus();
        if (next == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing status");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled order cannot change status");
        }

        // Simple rule: cannot go backward (except cancel)
        int cur = order.getOrderStatus() == null ? 0 : order.getOrderStatus().ordinal();
        int nxt = next.ordinal();
        if (next != OrderStatus.CANCELLED && nxt < cur) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move status backward");
        }

        order.setOrderStatus(next);
        if (next == OrderStatus.SHIPPING) {
            order.setShippedAt(LocalDateTime.now());
        }
        if (next == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
            // cộng điểm cho khách
            congDiemChoKhach(order);
        }

        recalcOrderTotals(order);
        return toDetailDTO(orderRepo.save(order));
    }

    @AuditAction(module = "ORDER", action = "DELETE", entity = "Order", description = "Xóa (ẩn) đơn hàng ID: #{#id}")
    @Transactional
    public void delete(Long id) {
        Order order = getOrderOr404(id);
        order.setDeleted(true);
        order.setDeletedAt(LocalDateTime.now());
        orderRepo.save(order);
    }

    @AuditAction(module = "ORDER", action = "ADD_ITEMS", entity = "Order", description = "Thêm sản phẩm mới vào đơn hàng ID: #{#orderId}")
    @Transactional
    public OrderDetailDTO addItems(Long orderId, List<OrderItemCreateRequest> itemsReq) {

        Order order = getOrderOr404(orderId);
        assertEditable(order);

        String customerType = "THUONG";

        if (order.getCustomer() != null && order.getCustomer().getLoaiKhach() != null) {
            customerType = order.getCustomer().getLoaiKhach();
        }

        for (OrderItemCreateRequest ir : itemsReq) {

            OrderItem it = new OrderItem();

            it.setOrder(order);
            it.setVariant(em.getReference(ProductVariant.class, ir.getVariantId()));
            it.setQuantity(ir.getQuantity());

            BigDecimal finalPrice =
                    pricingCalculationService.calculateFinalPrice(
                            ir.getVariantId(),
                            customerType
                    );

            it.setUnitPrice(finalPrice);

            if (ir.getLineDiscountAmount() != null)
                it.setLineDiscountAmount(nz(ir.getLineDiscountAmount()));

            it.setSkuSnapshot(ir.getSkuSnapshot());
            it.setProductNameSnapshot(ir.getProductNameSnapshot());

            calcLine(it);

            order.getItems().add(it);
        }

        recalcOrderTotals(order);

        return toDetailDTO(orderRepo.save(order));
    }

    @AuditAction(module = "ORDER", action = "UPDATE_ITEM_QTY", entity = "Order", description = "Cập nhật số lượng sản phẩm ID: #{#itemId} trong đơn hàng ID: #{#orderId} thành #{#req.quantity}")
    @Transactional
    public OrderDetailDTO updateItemQty(Long orderId, Long itemId, UpdateItemQuantityRequest req) {

        Order order = getOrderOr404(orderId);
        assertEditable(order);

        OrderItem item = order.getItems().stream()
                .filter(i -> Objects.equals(i.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order item not found: " + itemId
                ));

        // ===== LẤY LOẠI KHÁCH =====
        String customerType = "THUONG";

        if (order.getCustomer() != null && order.getCustomer().getLoaiKhach() != null) {
            customerType = order.getCustomer().getLoaiKhach();
        }

        // ===== TÍNH GIÁ THEO PROMOTION =====
        BigDecimal finalPrice =
                pricingCalculationService.calculateFinalPrice(
                        item.getVariantId(),
                        customerType
                );

        item.setUnitPrice(finalPrice);

        item.setQuantity(req.getQuantity());

        calcLine(item);
        recalcOrderTotals(order);

        return toDetailDTO(orderRepo.save(order));
    }

    @AuditAction(module = "ORDER", action = "RETURN", entity = "Order", description = "Xử lý hoàn trả cho đơn hàng ID: #{#orderId}, trạng thái: #{#req.returnStatus}")
    @Transactional
    public OrderDetailDTO applyReturn(Long orderId, ReturnOrderRequest req) {
        Order order = getOrderOr404(orderId);
        assertReturnable(order);

        order.setReturnStatus(req.getReturnStatus());
        order.setReturnNote(req.getReturnNote());
        order.setReturnedAt(LocalDateTime.now());

        if (req.getItems() != null) {
            Map<Long, ReturnItemRequest> map = req.getItems().stream()
                    .collect(Collectors.toMap(ReturnItemRequest::getOrderItemId, x -> x, (a, b) -> b));

            for (OrderItem it : order.getItems()) {
                ReturnItemRequest r = map.get(it.getId());
                if (r != null) {
                    int returnedQty = Math.max(0, r.getReturnedQuantity());
                    if (it.getQuantity() != null && returnedQty > it.getQuantity()) returnedQty = it.getQuantity();
                    it.setReturnedQuantity(returnedQty);
                    it.setReturnNote(r.getReturnNote());
                    it.setReturnedAt(LocalDateTime.now());
                }
            }
        }

        BigDecimal returnedAmount = BigDecimal.ZERO;
        for (OrderItem it : order.getItems()) {
            int rq = it.getReturnedQuantity() == null ? 0 : it.getReturnedQuantity();
            if (rq > 0) {
                BigDecimal qty = BigDecimal.valueOf(it.getQuantity() == null ? 0 : it.getQuantity());
                BigDecimal netUnit;
                if (qty.signum() > 0 && it.getLineTotalAmount() != null) {
                    netUnit = nz(it.getLineTotalAmount()).divide(qty, 2, RoundingMode.HALF_UP);
                } else {
                    netUnit = nz(it.getUnitPrice());
                }
                if (netUnit.signum() < 0) netUnit = BigDecimal.ZERO;
                returnedAmount = returnedAmount.add(netUnit.multiply(BigDecimal.valueOf(rq)));
            }
        }
        if (req.getReturnedAmountOverride() != null) {
            returnedAmount = nz(req.getReturnedAmountOverride());
        }

        order.setReturnedAmount(returnedAmount);

        // Checklist: cập nhật tồn kho khi hoàn trả (cộng lại tồn theo số lượng trả)
        if (order.getItems() != null) {
            for (OrderItem it : order.getItems()) {
                int rq = it.getReturnedQuantity() == null ? 0 : it.getReturnedQuantity();
                if (rq > 0) {
                    adjustVariantStockBestEffort(it.getVariantId(), rq);
                }
            }
        }

        // Checklist: cập nhật trạng thái đơn hàng sau hoàn trả (tuỳ rule)
        if (req.getReturnStatus() == ReturnStatus.COMPLETED
                && order.getOrderStatus() == OrderStatus.SHIPPING) {
            order.setOrderStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
        }

        recalcOrderTotals(order);

        Order saved = orderRepo.save(order);

        // Trừ điểm khi hoàn trả
        truDiemKhiHoanTra(saved);

        return toDetailDTO(saved);
    }

    // Checklist: hiển thị báo cáo đơn hàng hoàn trả
    @Transactional(readOnly = true)
    public List<ReturnReportDTO> returnReport(Optional<ReturnStatus> statusOpt) {
        List<Order> orders = statusOpt
                .map(s -> orderRepo.findAllByReturnStatusAndDeletedFalseOrderByCreatedAtDesc(s))
                .orElseGet(() -> orderRepo.findAllByReturnStatusIsNotNullAndDeletedFalseOrderByCreatedAtDesc());

        return orders.stream().map(o -> {
            ReturnReportDTO r = new ReturnReportDTO();
            r.setOrderId(o.getId());
            r.setOrderCode(o.getOrderCode());
            r.setReturnStatus(o.getReturnStatus());
            r.setTotalAmount(o.getTotalAmount());
            r.setReturnedAmount(o.getReturnedAmount());
            r.setFinalAmount(o.getFinalAmount());
            r.setReturnedAt(o.getReturnedAt());
            return r;
        }).collect(Collectors.toList());
    }

    private void calcLine(OrderItem it) {
        BigDecimal unit = nz(it.getUnitPrice());
        BigDecimal qty = BigDecimal.valueOf(it.getQuantity() == null ? 0 : it.getQuantity());
        BigDecimal discount = nz(it.getLineDiscountAmount());
        BigDecimal line = unit.multiply(qty).subtract(discount);
        if (line.signum() < 0) line = BigDecimal.ZERO;
        it.setLineTotalAmount(line);
    }

    private void recalcOrderTotals(Order order) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (order.getItems() != null) {
            for (OrderItem it : order.getItems()) {
                subtotal = subtotal.add(nz(it.getLineTotalAmount()));
            }
        }
        order.setSubtotalAmount(subtotal);

        // ưu đãi cho khách
        Customer customer = order.getCustomer();

        if (customer != null) {

            int giamTheoDiem = customer.getUuDaiTheoDiem() == null ? 0 : customer.getUuDaiTheoDiem();
            int giamTheoNhom = customer.getUuDaiTheoNhom() == null ? 0 : customer.getUuDaiTheoNhom();

            // 🔥 chỉ chọn ưu đãi lớn nhất
            int discountPercent = Math.max(giamTheoDiem, giamTheoNhom);

            if (discountPercent > 0) {

                BigDecimal discount = subtotal
                        .multiply(BigDecimal.valueOf(discountPercent))
                        .divide(BigDecimal.valueOf(100));

                order.setDiscountAmount(discount);
            }
        }

        BigDecimal discount = nz(order.getDiscountAmount());
        BigDecimal shipping = nz(order.getShippingFee());
        BigDecimal total = subtotal.subtract(discount).add(shipping);
        if (total.signum() < 0) total = BigDecimal.ZERO;
        order.setTotalAmount(total);

        BigDecimal returned = nz(order.getReturnedAmount());
        BigDecimal finalAmount = total.subtract(returned);
        if (finalAmount.signum() < 0) finalAmount = BigDecimal.ZERO;
        order.setFinalAmount(finalAmount);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String generateOrderCode() {
        String date = LocalDate.now().toString().replace("-", "");
        String rnd = String.valueOf(new Random().nextInt(900000) + 100000);
        String code = "ORD" + date + rnd;
        if (orderRepo.existsByOrderCode(code)) {
            return generateOrderCode();
        }
        return code;
    }

    private OrderSummaryDTO toSummaryDTO(Order o) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.setId(o.getId());
        dto.setOrderCode(o.getOrderCode());
        dto.setOrderStatus(o.getOrderStatus());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setFinalAmount(o.getFinalAmount());
        dto.setRevenue(calcRevenue(o));
        dto.setCreatedAt(o.getCreatedAt());
        return dto;
    }

    private OrderDetailDTO toDetailDTO(Order o) {
        OrderDetailDTO dto = new OrderDetailDTO();
        dto.setId(o.getId());
        dto.setOrderCode(o.getOrderCode());
        dto.setCustomerId(o.getCustomer() == null ? null : o.getCustomer().getId());
        dto.setCreatedById(o.getCreatedBy() == null ? null : o.getCreatedBy().getId());
        dto.setChannel(o.getChannel());
        dto.setOrderStatus(o.getOrderStatus());
        dto.setPaymentStatus(o.getPaymentStatus());
        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setReturnStatus(o.getReturnStatus());

        dto.setSubtotalAmount(o.getSubtotalAmount());
        dto.setDiscountAmount(o.getDiscountAmount());
        dto.setShippingFee(o.getShippingFee());
        dto.setTotalAmount(o.getTotalAmount());
        dto.setReturnedAmount(o.getReturnedAmount());
        dto.setFinalAmount(o.getFinalAmount());
        dto.setRevenue(calcRevenue(o));

        dto.setNote(o.getNote());
        dto.setReturnNote(o.getReturnNote());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getUpdatedAt());

        List<OrderItemDTO> items = new ArrayList<>();
        if (o.getItems() != null) {
            for (OrderItem it : o.getItems()) {
                OrderItemDTO itDto = new OrderItemDTO();
                itDto.setId(it.getId());
                // yêu cầu: OrderItem của bạn nên có getVariantId() @Transient
                itDto.setVariantId(it.getVariantId());
                itDto.setSkuSnapshot(it.getSkuSnapshot());
                itDto.setProductNameSnapshot(it.getProductNameSnapshot());
                itDto.setUnitPrice(it.getUnitPrice());
                itDto.setQuantity(it.getQuantity());
                itDto.setLineDiscountAmount(it.getLineDiscountAmount());
                itDto.setLineTotalAmount(it.getLineTotalAmount());
                itDto.setReturnedQuantity(it.getReturnedQuantity());
                itDto.setReturnNote(it.getReturnNote());
                items.add(itDto);
            }
        }
        dto.setItems(items);
        return dto;
    }

    // Checklist: tính tổng doanh thu từng đơn / cập nhật doanh thu sau hoàn trả
    private BigDecimal calcRevenue(Order o) {
        if (o == null) return BigDecimal.ZERO;
        if (o.getOrderStatus() == OrderStatus.COMPLETED) {
            return nz(o.getFinalAmount());
        }
        return BigDecimal.ZERO;
    }

    // Doanh thu khách
    public List<CustomerSpendingDTO> getCustomerSpending(){

        List<Object[]> data = orderRepo.getCustomerSpending();
        return data.stream()
                .map(o -> new CustomerSpendingDTO(
                        (Long)o[0],
                        (String)o[1],
                        (BigDecimal)o[2]
                ))
                .toList();
    }

    // Top khách hàng
    public List<CustomerSpendingDTO> getTopCustomers(){

        return getCustomerSpending()
                .stream()
                .limit(10)
                .toList();
    }

    // Khách hàng lâu không hoạt động
    public List<InactiveCustomerDTO> getInactiveCustomers() {

        List<Object[]> data = orderRepo.getLastOrderTime();

        return data.stream()
                .map(r -> {

                    Long id = (Long) r[0];
                    String name = (String) r[1];
                    LocalDateTime lastOrder = (LocalDateTime) r[2];

                    long days = ChronoUnit.DAYS.between(lastOrder, LocalDateTime.now());

                    return new InactiveCustomerDTO(id, name, days);

                })
                .filter(c -> c.getDaysSinceLastOrder() > 30)
                .toList();
    }

    //Cộng điểm khi mua hàng
    @Transactional
    public void congDiemChoKhach(Order order) {

        Customer customer = order.getCustomer();

        if (customer != null && order.getFinalAmount() != null) {

            int diemCong = order.getFinalAmount()
                    .divide(BigDecimal.valueOf(10000))
                    .intValue();

            customer.setDiemTichLuy(
                    customer.getDiemTichLuy() + diemCong
            );

            customerRepo.save(customer);
        }
    }

    // Trừ điểm khi hoàn trả ( đang fix)
    @Transactional
    public void truDiemKhiHoanTra(Order order) {

        Customer customer = order.getCustomer();
        if (customer == null) return;

        BigDecimal returned = order.getReturnedAmount();
        if (returned == null || returned.compareTo(BigDecimal.ZERO) <= 0) return;

        int diemTru = returned
                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN)
                .intValue();

        int diemHienTai = customer.getDiemTichLuy() == null ? 0 : customer.getDiemTichLuy();

        int diemMoi = diemHienTai - diemTru;
        if (diemMoi < 0) diemMoi = 0;

        customer.setDiemTichLuy(diemMoi);

        customerRepo.save(customer);

        System.out.println("Trừ điểm khách: -" + diemTru);
    }
}

