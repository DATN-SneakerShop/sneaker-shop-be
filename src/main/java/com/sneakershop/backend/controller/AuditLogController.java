package com.sneakershop.backend.controller;

import com.sneakershop.backend.dto.AuditLogDTO;
import com.sneakershop.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/management/logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditLogController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> getAllLogs() {
        List<AuditLogDTO> dtos = userService.getAllAuditLogs().stream().map(log -> {
            AuditLogDTO d = new AuditLogDTO();
            d.setId(log.getId());
            d.setAction(log.getAction());
            d.setSummary(log.getSummary());
            d.setTimestamp(log.getCreatedAt());
            d.setIpAddress(log.getIpAddress());
            d.setEntityName(log.getEntityName());
            d.setPerformedBy(log.getPerformedBy() != null ? log.getPerformedBy().getFullName() : "Hệ thống");
            return d;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}