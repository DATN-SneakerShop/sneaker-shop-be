package com.sneakershop.backend;


import com.sneakershop.backend.entity.login.AuditLog;
import com.sneakershop.backend.entity.login.User;

import com.sneakershop.backend.repository.login.AuditLogRepository;
import com.sneakershop.backend.service.login.UserService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditRepo;
    private final UserService userService;
    private final HttpServletRequest request;

    @AfterReturning("@annotation(auditAction)")
    public void logAction(JoinPoint jp, AuditAction auditAction) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);

        AuditLog log = new AuditLog();
        log.setModule(auditAction.module());
        log.setAction(auditAction.action());
        log.setEntityName(auditAction.entity());
        log.setSummary(jp.getSignature().toShortString());
        log.setIpAddress(request.getRemoteAddr());
        log.setPerformedBy(user);
        log.setCreatedAt(LocalDateTime.now());

        auditRepo.save(log);
    }
}