package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.customer.CustomerSpendingDTO;
import com.sneakershop.backend.dto.customer.InactiveCustomerDTO;
import com.sneakershop.backend.dto.order.*;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import com.sneakershop.backend.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDetailDTO> create(@Valid @RequestBody CreateOrderRequest req) {
        return new ResponseEntity<>(orderService.create(req), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryDTO>> list(
            @RequestParam(name = "status", required = false) OrderStatus status
    ) {
        return ResponseEntity.ok(orderService.list(Optional.ofNullable(status)));
    }

    // Checklist: hiển thị đơn hàng theo khách hàng
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<OrderSummaryDTO>> listByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.listByCustomer(customerId));
    }

    // Checklist: hiển thị đơn hàng theo nhân viên bán
    @GetMapping("/by-staff/{createdById}")
    public ResponseEntity<List<OrderSummaryDTO>> listByStaff(@PathVariable Long createdById) {
        return ResponseEntity.ok(orderService.listByStaff(createdById));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailDTO> detail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.detail(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDetailDTO> update(@PathVariable Long id, @RequestBody UpdateOrderRequest req) {
        return ResponseEntity.ok(orderService.update(id, req));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDetailDTO> cancel(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest req) {
        return ResponseEntity.ok(orderService.cancel(id, req));
    }

    // Checklist: thêm trạng thái "Đang giao" / "Hoàn tất giao hàng"
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDetailDTO> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest req) {
        return ResponseEntity.ok(orderService.updateStatus(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderDetailDTO> addItems(@PathVariable Long id, @Valid @RequestBody List<OrderItemCreateRequest> items) {
        return ResponseEntity.ok(orderService.addItems(id, items));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderDetailDTO> updateItemQty(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemQuantityRequest req
    ) {
        return ResponseEntity.ok(orderService.updateItemQty(id, itemId, req));
    }

    @PostMapping("/{id}/returns")
    public ResponseEntity<OrderDetailDTO> applyReturn(@PathVariable Long id, @Valid @RequestBody ReturnOrderRequest req) {
        return ResponseEntity.ok(orderService.applyReturn(id, req));
    }

    // Checklist: báo cáo đơn hàng hoàn trả
    @GetMapping("/returns/report")
    public ResponseEntity<List<ReturnReportDTO>> returnReport(
            @RequestParam(name = "status", required = false) ReturnStatus status
    ) {
        return ResponseEntity.ok(orderService.returnReport(Optional.ofNullable(status)));
    }

    // Checklist: tạo tính năng in đơn hàng PDF (trả HTML để trình duyệt Print -> Save PDF)
    @GetMapping(value = "/{id}/print", produces = "text/html; charset=UTF-8")
    public ResponseEntity<String> printHtml(@PathVariable Long id) {
        OrderDetailDTO dto = orderService.detail(id);

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='UTF-8'/>")
            .append("<style>body{font-family:Arial} table{border-collapse:collapse;width:100%} td,th{border:1px solid #ccc;padding:6px}</style>")
            .append("</head><body>")
            .append("<h2>Hóa đơn: ").append(dto.getOrderCode()).append("</h2>")
            .append("<p>Status: ").append(dto.getOrderStatus())
            .append(" | Payment: ").append(dto.getPaymentMethod())
            .append(" | Revenue: ").append(dto.getRevenue())
            .append("</p>")
            .append("<table><thead><tr><th>Sản phẩm</th><th>SL</th><th>Đơn giá</th><th>Thành tiền</th></tr></thead><tbody>");

        if (dto.getItems() != null) {
            for (OrderItemDTO it : dto.getItems()) {
                String name = it.getProductNameSnapshot() != null ? it.getProductNameSnapshot() : ("Variant#" + it.getVariantId());
                html.append("<tr><td>").append(name).append("</td>")
                    .append("<td>").append(it.getQuantity()).append("</td>")
                    .append("<td>").append(it.getUnitPrice()).append("</td>")
                    .append("<td>").append(it.getLineTotalAmount()).append("</td></tr>");
            }
        }

        html.append("</tbody></table>")
            .append("<p>Subtotal: ").append(dto.getSubtotalAmount()).append("</p>")
            .append("<p>Discount: ").append(dto.getDiscountAmount()).append("</p>")
            .append("<p>Shipping: ").append(dto.getShippingFee()).append("</p>")
            .append("<p><b>Total: ").append(dto.getTotalAmount()).append("</b></p>")
            .append("<p>Returned: ").append(dto.getReturnedAmount()).append("</p>")
            .append("<p><b>Final: ").append(dto.getFinalAmount()).append("</b></p>")
            .append("</body></html>");

        return ResponseEntity.ok(html.toString());
    }

    // Chi tiêu khách hàng
    @GetMapping("/customer-spending")
    public ResponseEntity<List<CustomerSpendingDTO>> customerSpending(){
        return ResponseEntity.ok(orderService.getCustomerSpending());
    }

    // Top khách hàng
    @GetMapping("/top-customers")
    public ResponseEntity<List<CustomerSpendingDTO>> topCustomers(){
        return ResponseEntity.ok(orderService.getTopCustomers());
    }

    // Khách hàng lâu chưa hoạt động (đang fix)
    @GetMapping("/inactive-customers")
    public List<InactiveCustomerDTO> getInactiveCustomers() {
        return orderService.getInactiveCustomers();
    }
}
