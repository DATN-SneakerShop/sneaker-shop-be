package com.sneakershop.backend.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/management/logs")
@RequiredArgsConstructor
public class SystemAuditLogController {

    private final SystemAuditLogService systemAuditLogService;

    // API lấy danh sách toàn bộ Log (đã được sắp xếp mới nhất lên đầu ở tầng Service)
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<SystemAuditLog>> getAllLogs() {
        List<SystemAuditLog> logs = systemAuditLogService.getAllLogs();
        return ResponseEntity.ok(logs);
    }
}