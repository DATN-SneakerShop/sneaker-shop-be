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

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditRepository;

    @Transactional
    public void registerLocal(UserRequest request, String ip) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setLoaiDangNhap("LOCAL");
        user.setStatus("ACTIVE");

        // FIX: Luôn gán quyền CUSTOMER khi đăng ký mới
        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Lỗi: Role CUSTOMER chưa có trong DB!"));
        user.setRoles(Set.of(customerRole));

        User savedUser = userRepository.save(user);
        saveAuditLog("REGISTER", savedUser.getId(), "Đăng ký tài khoản mới", ip, savedUser);
    }

    @Transactional
    public User processGoogleAuth(String googleId, String email, String name, String ip) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            if ("DELETED".equals(user.getStatus())) {
                user.setStatus("ACTIVE");
            }
            user.setGoogleId(googleId);
            user.setFullName(name);
        } else {
            user = new User();
            String defaultUsername = email.split("@")[0];
            user.setUsername(userRepository.existsByUsername(defaultUsername) ? email : defaultUsername);
            user.setEmail(email);
            user.setFullName(name);
            user.setGoogleId(googleId);
            user.setStatus("ACTIVE");
            user.setLoaiDangNhap("GOOGLE");
            user.setPasswordHash(passwordEncoder.encode("GOOGLE_PWD_" + googleId));

            // FIX: Gán quyền CUSTOMER cho user Google mới
            Role customerRole = roleRepository.findByCode("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Lỗi: Role CUSTOMER chưa có trong DB!"));
            user.setRoles(Set.of(customerRole));
        }

        User savedUser = userRepository.save(user);
        saveAuditLog("GOOGLE_LOGIN", savedUser.getId(), "Đăng nhập Google", ip, savedUser);
        return savedUser;
    }

    private void saveAuditLog(String action, Long entityId, String summary, String ip, User performedBy) {
        AuditLog log = new AuditLog();
        log.setModule("AUTH");
        log.setAction(action);
        log.setEntityName("User");
        log.setEntityId(entityId);
        log.setSummary(summary);
        log.setIpAddress(ip);
        log.setPerformedBy(performedBy);
        auditRepository.save(log);
    }
}