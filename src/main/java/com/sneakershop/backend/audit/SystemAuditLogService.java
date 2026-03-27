package com.sneakershop.backend.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SystemAuditLogService {

    private final SystemAuditLogRepository repository;
    private final JavaMailSender mailSender; // 🔥 Đã thêm máy gửi Email

    public List<SystemAuditLog> getAllLogs() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<SystemAuditLog> filterLogs(String module, String action, String status, String username, LocalDateTime startDate, LocalDateTime endDate) {
        return repository.findAdvanced(module, action, status, username, startDate, endDate);
    }

    public List<Map<String, Object>> getUserLogReport() {
        List<Object[]> results = repository.getUserLogReport();
        List<Map<String, Object>> report = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("username", row[0] != null ? row[0].toString() : "GUEST");
            map.put("totalAction", row[1]);
            map.put("successCount", row[2] != null ? row[2] : 0L);
            map.put("failedCount", row[3] != null ? row[3] : 0L);
            report.add(map);
        }
        return report;
    }

    // 🔥 CẬP NHẬT: Gửi email nếu log ở mức DANGER hoặc ERROR
    public void logManual(String username, String ip, String module, String action, String entity, String summary, String status, String error, String logLevel) {
        SystemAuditLog log = new SystemAuditLog();
        log.setUsername(username);
        log.setIpAddress(ip);
        log.setModule(module);
        log.setAction(action);
        log.setEntityName(entity);
        log.setSummary(summary);
        log.setStatus(status);
        log.setErrorMessage(error);

        String finalLogLevel = logLevel != null ? logLevel : "INFO";
        log.setLogLevel(finalLogLevel);
        repository.save(log);

        // 🔥 YÊU CẦU 5: NẾU CÓ XÂM NHẬP HOẶC LỖI HỆ THỐNG -> GỬI EMAIL NGAY
        if ("DANGER".equals(finalLogLevel) || "ERROR".equals(finalLogLevel)) {
            sendAlertEmailAsync(log);
        }
    }

    // Chạy đa luồng (Thread) ngầm để việc gửi email không làm lag/chậm API của người dùng
    private void sendAlertEmailAsync(SystemAuditLog log) {
        new Thread(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();

                // ⚠️ THAY BẰNG EMAIL THỰC TẾ CỦA ADMIN (Hoặc email của mày để test)
                message.setTo("admin_sneakershop@gmail.com");

                message.setSubject("🚨 [CẢNH BÁO BẢO MẬT] Hệ thống Sneaker Shop");
                message.setText("Hệ thống vừa ghi nhận một hành vi bất thường hoặc lỗi nghiêm trọng:\n\n" +
                        "⏰ Thời gian: " + LocalDateTime.now() + "\n" +
                        "👤 User thực hiện: " + log.getUsername() + "\n" +
                        "🌐 IP Address: " + log.getIpAddress() + "\n" +
                        "⚠️ Mức độ: " + log.getLogLevel() + "\n" +
                        "📍 Module: " + log.getModule() + "\n" +
                        "🛠 Hành động: " + log.getAction() + "\n" +
                        "📄 Chi tiết: " + log.getSummary() + "\n" +
                        "❌ Mã lỗi: " + (log.getErrorMessage() != null ? log.getErrorMessage() : "Không có") + "\n\n" +
                        "Vui lòng truy cập trang Quản trị để kiểm tra ngay lập tức!");
                mailSender.send(message);
            } catch (Exception e) {
                System.err.println("❌ Không thể gửi email cảnh báo bảo mật: " + e.getMessage());
            }
        }).start();
    }
}