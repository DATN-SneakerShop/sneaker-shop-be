package com.sneakershop.backend.controller.order;

import com.sneakershop.backend.dto.order.*;
import com.sneakershop.backend.entity.order.enums.OrderStatus;
import com.sneakershop.backend.entity.order.enums.ReturnStatus;
import com.sneakershop.backend.entity.order.enums.SalesChannel;
import com.sneakershop.backend.service.order.OrderService;
import com.sneakershop.backend.service.customer.CustomerAnalyticsService;
import com.sneakershop.backend.dto.customer.CustomerSpendingDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final CustomerAnalyticsService customerAnalyticsService;

    @PostMapping
    public ResponseEntity<OrderDetailDTO> create(@Valid @RequestBody CreateOrderRequest req) {
        return new ResponseEntity<>(orderService.create(req), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryDTO>> list(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "channel", required = false) SalesChannel channel,
            @RequestParam(name = "customerId", required = false) Long customerId,
            @RequestParam(name = "createdById", required = false) Long createdById,
            @RequestParam(name = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        return ResponseEntity.ok(
                orderService.list(
                        Optional.ofNullable(status),
                        Optional.ofNullable(channel),
                        Optional.ofNullable(customerId),
                        Optional.ofNullable(createdById),
                        Optional.ofNullable(dateFrom),
                        Optional.ofNullable(dateTo),
                        Optional.ofNullable(keyword)
                )
        );
    }

    @GetMapping("/by-date")
    public ResponseEntity<List<OrderSummaryDTO>> listByDate(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(orderService.listByDate(date));
    }

    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<OrderSummaryDTO>> listByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.listByCustomer(customerId));
    }

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

    @GetMapping("/returns/report")
    public ResponseEntity<List<ReturnReportDTO>> returnReport(
            @RequestParam(name = "status", required = false) ReturnStatus status
    ) {
        return ResponseEntity.ok(orderService.returnReport(Optional.ofNullable(status)));
    }

    @GetMapping("/stats/by-staff")
    public ResponseEntity<List<StaffOrderStatisticDTO>> statsByStaff() {
        return ResponseEntity.ok(orderService.statsByStaff());
    }

    @GetMapping("/stats/revenue/by-customer")
    public ResponseEntity<List<CustomerRevenueDTO>> revenueByCustomer() {
        return ResponseEntity.ok(orderService.revenueByCustomer());
    }

    @GetMapping("/stats/revenue/daily")
    public ResponseEntity<List<DailyRevenueDTO>> revenueDaily(
            @RequestParam(name = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(orderService.revenueDaily(dateFrom, dateTo));
    }

    @GetMapping("/stats/revenue/weekly")
    public ResponseEntity<List<WeeklyRevenueDTO>> revenueWeekly(
            @RequestParam(name = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(orderService.revenueWeekly(dateFrom, dateTo));
    }

    @GetMapping("/stats/revenue/monthly")
    public ResponseEntity<List<MonthlyRevenueDTO>> revenueMonthly(
            @RequestParam(name = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(orderService.revenueMonthly(dateFrom, dateTo));
    }

    @GetMapping("/stats/top-products")
    public ResponseEntity<List<BestSellingProductDTO>> bestSellingProducts() {
        return ResponseEntity.ok(orderService.bestSellingProducts());
    }

    @GetMapping("/stats/returned-products")
    public ResponseEntity<List<ReturnedProductStatisticDTO>> returnedProducts() {
        return ResponseEntity.ok(orderService.returnedProducts());
    }


    @GetMapping("/customer-spending")
    public ResponseEntity<List<CustomerSpendingDTO>> customerSpending() {
        return ResponseEntity.ok(customerAnalyticsService.spending());
    }

    @GetMapping("/top-customers")
    public ResponseEntity<List<CustomerSpendingDTO>> topCustomers(
            @RequestParam(name = "limit", required = false, defaultValue = "3") Integer limit
    ) {
        return ResponseEntity.ok(customerAnalyticsService.topCustomers(limit));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<OrderDashboardDTO> dashboard() {
        return ResponseEntity.ok(orderService.dashboard());
    }

    @GetMapping(value = "/{id}/print", produces = "text/html; charset=UTF-8")
    public ResponseEntity<String> printHtml(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.buildPrintHtml(id));
    }

    @GetMapping(value = "/{id}/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportOrderPdf(@PathVariable Long id) {
        byte[] pdf = orderService.exportSingleOrderPdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("order-" + id + ".pdf", StandardCharsets.UTF_8)
                        .build()
        );

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/export-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportOrdersPdf(
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "channel", required = false) SalesChannel channel,
            @RequestParam(name = "customerId", required = false) Long customerId,
            @RequestParam(name = "createdById", required = false) Long createdById,
            @RequestParam(name = "dateFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        byte[] pdf = orderService.exportOrdersPdf(
                Optional.ofNullable(status),
                Optional.ofNullable(channel),
                Optional.ofNullable(customerId),
                Optional.ofNullable(createdById),
                Optional.ofNullable(dateFrom),
                Optional.ofNullable(dateTo),
                Optional.ofNullable(keyword)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("orders-report.pdf", StandardCharsets.UTF_8)
                        .build()
        );

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/email-preview")
    public ResponseEntity<OrderEmailPreviewDTO> emailPreview(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.emailPreview(id));
    }

    @PostMapping("/{id}/send-confirmation-email")
    public ResponseEntity<OrderEmailPreviewDTO> sendConfirmationEmail(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.markEmailSent(id));
    }

}