package com.sneakershop.backend.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/management/logs")
@RequiredArgsConstructor
public class SystemAuditLogController {

    private final SystemAuditLogService systemAuditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<SystemAuditLog>> getAllLogs() {
        List<SystemAuditLog> logs = systemAuditLogService.getAllLogs();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<SystemAuditLog>> filterLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return ResponseEntity.ok(systemAuditLogService.filterLogs(module, action, status, username, startDate, endDate));
    }

    @GetMapping("/report/user")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getUserLogReport() {
        return ResponseEntity.ok(systemAuditLogService.getUserLogReport());
    }
}