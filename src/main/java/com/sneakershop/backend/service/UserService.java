package com.sneakershop.backend.service;

import com.sneakershop.backend.dto.UserRequest;
import com.sneakershop.backend.entity.AuditLog;
import com.sneakershop.backend.entity.Role;
import com.sneakershop.backend.entity.User;
import com.sneakershop.backend.repository.AuditLogRepository;
import com.sneakershop.backend.repository.RoleRepository;
import com.sneakershop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditRepository;
    private final PasswordEncoder passwordEncoder;

    // Lấy tất cả user hiện có trong DB
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void createUser(UserRequest request, String ip, User admin) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setStatus("ACTIVE");

        if (request.getRoleCodes() != null) {
            Set<Role> roles = request.getRoleCodes().stream()
                    .map(code -> roleRepository.findByCode(code)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quyền " + code)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        saveAuditLog("CREATE", savedUser.getId(), "Tạo tài khoản: " + savedUser.getUsername(), ip, admin);
    }

    @Transactional
    public void updateUser(Long id, UserRequest request, String ip, User admin) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // Kiểm tra trùng email nếu đổi email
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email mới đã được sử dụng!");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // Cập nhật Roles nếu cần
        if (request.getRoleCodes() != null) {
            Set<Role> roles = request.getRoleCodes().stream()
                    .map(code -> roleRepository.findByCode(code)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quyền " + code)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        userRepository.save(user);
        saveAuditLog("UPDATE", id, "Cập nhật user: " + user.getUsername(), ip, admin);
    }

    @Transactional
    public void deleteUser(Long userId, String ip, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // 1. Xử lý khóa ngoại trong AuditLog: Tìm các log mà user này đã thực hiện và set null
        List<AuditLog> logs = auditRepository.findByPerformedBy(user);
        for (AuditLog log : logs) {
            log.setPerformedBy(null);
        }
        auditRepository.saveAll(logs);

        // 2. Xóa sạch User khỏi DB
        userRepository.delete(user);

        // 3. Ghi log hành động xóa (admin thực hiện)
        saveAuditLog("HARD_DELETE", userId, "Đã xóa vĩnh viễn tài khoản: " + user.getUsername(), ip, admin);
    }

    private void saveAuditLog(String action, Long entityId, String summary, String ip, User performedBy) {
        AuditLog log = new AuditLog();
        log.setModule("USER_MANAGEMENT");
        log.setAction(action);
        log.setEntityName("User");
        log.setEntityId(entityId);
        log.setSummary(summary);
        log.setIpAddress(ip);
        log.setPerformedBy(performedBy);
        auditRepository.save(log);
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditRepository.findAllByOrderByCreatedAtDesc();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}