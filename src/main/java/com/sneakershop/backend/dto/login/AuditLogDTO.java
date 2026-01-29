package com.sneakershop.backend.dto.login;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String module;
    private String action;
    private String entityName;
    private String summary;
    private String ipAddress;
    private LocalDateTime timestamp;
    private String performedBy;
}