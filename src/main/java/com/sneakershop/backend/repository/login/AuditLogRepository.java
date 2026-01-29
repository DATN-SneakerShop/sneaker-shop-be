package com.sneakershop.backend.repository.login;

import com.sneakershop.backend.entity.login.AuditLog;
import com.sneakershop.backend.entity.login.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByModuleOrderByCreatedAtDesc(String module);

    List<AuditLog> findByPerformedByIdOrderByCreatedAtDesc(Long userId);

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findByPerformedBy(User user);
}