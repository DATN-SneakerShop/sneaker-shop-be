package com.sneakershop.backend.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemAuditLogService {

    private final SystemAuditLogRepository repository;

    // Lấy danh sách Log (Mới nhất lên đầu)
    public List<SystemAuditLog> getAllLogs() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // 🔥 HÀM GHI LOG THỦ CÔNG (Dành riêng cho AuthService khi User chưa đăng nhập)
    public void logManual(String username, String ip, String module, String action, String entity, String summary, String status, String error) {
        SystemAuditLog log = new SystemAuditLog();
        log.setUsername(username);
        log.setIpAddress(ip);
        log.setModule(module);
        log.setAction(action);
        log.setEntityName(entity);
        log.setSummary(summary);
        log.setStatus(status);
        log.setErrorMessage(error);
        repository.save(log);
    }
}