package com.sneakershop.backend.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, Long> {

    @Query("SELECT l FROM SystemAuditLog l WHERE " +
            "(:module IS NULL OR l.module = :module) AND " +
            "(:action IS NULL OR l.action = :action) AND " +
            "(:status IS NULL OR l.status = :status) AND " +
            "(:username IS NULL OR l.username = :username) AND " +
            "(:startDate IS NULL OR l.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR l.createdAt <= :endDate) " +
            "ORDER BY l.createdAt DESC")
    List<SystemAuditLog> findAdvanced(
            @Param("module") String module,
            @Param("action") String action,
            @Param("status") String status,
            @Param("username") String username,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT l.username AS username, COUNT(l) AS totalCount, " +
            "SUM(CASE WHEN l.status = 'SUCCESS' THEN 1 ELSE 0 END) AS successCount, " +
            "SUM(CASE WHEN l.status = 'FAILED' THEN 1 ELSE 0 END) AS failedCount " +
            "FROM SystemAuditLog l GROUP BY l.username ORDER BY totalCount DESC")
    List<Object[]> getUserLogReport();
}