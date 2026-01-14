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
    private final AuditLogRepository auditLogRepository;

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void createUser(UserRequest request, String ip, User admin) {
        // Check trùng username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        // Check trùng email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setStatus("ACTIVE");

        Set<Role> roles = request.getRoleCodes().stream()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quyền " + code)))
                .collect(Collectors.toSet());
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        saveAuditLog("CREATE", savedUser.getId(), "Tạo tài khoản: " + savedUser.getUsername(), ip, admin);
    }

    @Transactional
    public void updateUser(Long id, UserRequest request, String ip, User admin) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // VALIDATE: Check trùng email (trừ email hiện tại của chính nó)
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email mới đã được sử dụng bởi người khác!");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);
        saveAuditLog("UPDATE", id, "Cập nhật user: " + user.getUsername(), ip, admin);
    }

    @Transactional
    public void deleteUser(Long userId, String ip, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng để xóa"));
        userRepository.deleteById(userId);
        saveAuditLog("DELETE", userId, "Xóa người dùng: " + user.getUsername(), ip, admin);
    }

    // Hàm dùng chung để ghi log cho sạch code
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

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}