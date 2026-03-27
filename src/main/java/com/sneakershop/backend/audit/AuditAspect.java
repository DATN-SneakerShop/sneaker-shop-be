package com.sneakershop.backend.audit;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final SystemAuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    @AfterReturning(value = "@annotation(auditAction)", returning = "result")
    public void logSuccessAction(JoinPoint jp, AuditAction auditAction, Object result) {
        saveLog(jp, auditAction, "SUCCESS", null);
    }

    @AfterThrowing(value = "@annotation(auditAction)", throwing = "exception")
    public void logFailedAction(JoinPoint jp, AuditAction auditAction, Exception exception) {
        saveLog(jp, auditAction, "FAILED", exception.getMessage());
    }

    private void saveLog(JoinPoint jp, AuditAction auditAction, String status, String errorMessage) {
        try {
            String username = "GUEST";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                username = auth.getName();
            }

            SystemAuditLog log = new SystemAuditLog();
            log.setUsername(username);
            log.setIpAddress(request.getRemoteAddr());
            log.setModule(auditAction.module());
            log.setAction(auditAction.action());
            log.setEntityName(auditAction.entity());

            String summary = resolveDescription(jp, auditAction.description());
            log.setSummary(summary);

            log.setStatus(status);
            log.setErrorMessage(errorMessage);

            // 🔥 MỚI THÊM: Tự động phân loại INFO nếu SUCCESS, và ERROR nếu FAILED
            log.setLogLevel("SUCCESS".equals(status) ? "INFO" : "ERROR");

            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi ghi Audit Log: " + e.getMessage());
        }
    }

    private String resolveDescription(JoinPoint jp, String description) {
        if (description == null || !description.contains("#{")) {
            return description.isEmpty() ? jp.getSignature().toShortString() : description;
        }
        try {
            ExpressionParser parser = new SpelExpressionParser();
            StandardEvaluationContext context = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) jp.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = jp.getArgs();

            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            return parser.parseExpression(description, new TemplateParserContext()).getValue(context, String.class);
        } catch (Exception e) {
            return description;
        }
    }
}