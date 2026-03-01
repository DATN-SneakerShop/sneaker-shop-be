package com.sneakershop.backend.controller.customer;

import com.sneakershop.backend.entity.customer.CustomerAuditLog;
import com.sneakershop.backend.repository.customer.CustomerAuditLogRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer-audit-logs")
public class CustomerAuditLogController {

    private final CustomerAuditLogRepository repository;

    public CustomerAuditLogController(CustomerAuditLogRepository repository) {
        this.repository = repository;
    }

    // Lấy toàn bộ log
    @GetMapping
    public List<CustomerAuditLog> getAll() {
        return repository.findAll();
    }

    // Lấy log theo customerId
    @GetMapping("/customer/{id}")
    public List<CustomerAuditLog> getByCustomer(@PathVariable Long id) {
        return repository.findByCustomerIdOrderByTimestampDesc(id);
    }
}
