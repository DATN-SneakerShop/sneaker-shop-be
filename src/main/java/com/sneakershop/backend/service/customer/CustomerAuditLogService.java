package com.sneakershop.backend.service.customer;

import com.sneakershop.backend.entity.customer.CustomerAuditLog;
import com.sneakershop.backend.repository.customer.CustomerAuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class CustomerAuditLogService {

    private final CustomerAuditLogRepository repo;

    public CustomerAuditLogService(CustomerAuditLogRepository repo) {
        this.repo = repo;
    }

    public void log(
            Long customerId,
            String action,
            String summary,
            String username,
            String ip
    ) {
        CustomerAuditLog log = new CustomerAuditLog();
        log.setCustomerId(customerId);
        log.setAction(action);
        log.setEntityName("Khách hàng");
        log.setSummary(summary);
        log.setPerformedBy(username);
        log.setIpAddress(ip);

        repo.save(log);
    }

}

