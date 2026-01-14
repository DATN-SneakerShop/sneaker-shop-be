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
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditRepository;

    @Transactional
    public void registerLocal(UserRequest request, String ip) {
        // 1. Kiểm tra username đã tồn tại chưa
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + request.getUsername() + "' đã tồn tại!");
        }

        // 2. Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' đã được sử dụng bởi tài khoản khác!");
        }

        // 3. Tạo mới User
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setLoaiDangNhap("LOCAL");
        user.setStatus("ACTIVE");

        // 4. Gán Role mặc định
        Role defaultRole = roleRepository.findByCode("SALES")
                .orElseThrow(() -> new RuntimeException("Lỗi: Vai trò SALES chưa được cấu hình trong hệ thống!"));
        user.setRoles(Set.of(defaultRole));

        User savedUser = userRepository.save(user);
        saveAuditLog("REGISTER", savedUser.getId(), "Đăng ký tài khoản hệ thống", ip, savedUser);
    }

    @Transactional
    public User processGoogleAuth(String googleId, String email, String name, String ip) {
        // 1. Kiểm tra xem email này đã có trong hệ thống chưa
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Nếu user đã tồn tại nhưng chưa có googleId (đăng ký local trước đó), thì cập nhật ID Google vào
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
                user.setLoaiDangNhap("GOOGLE");
                userRepository.save(user);
            }
            return user;
        }

        // 2. Nếu chưa có email này trong DB -> Tạo mới hoàn toàn
        User newUser = new User();

        // Tạo username duy nhất từ email (VD: ndhieu1610 từ ndhieu1610@gmail.com)
        String baseUsername = email.split("@")[0];
        if (userRepository.existsByUsername(baseUsername)) {
            baseUsername = baseUsername + "_" + googleId.substring(0, 4);
        }

        newUser.setUsername(baseUsername);
        newUser.setEmail(email);
        newUser.setFullName(name);
        newUser.setGoogleId(googleId);
        newUser.setLoaiDangNhap("GOOGLE");

        // QUAN TRỌNG: passwordHash nullable = false nên phải set giá trị ngẫu nhiên
        newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setStatus("ACTIVE");

        // Gán Role mặc định là SALES (giống đăng ký thường)
        Role defaultRole = roleRepository.findByCode("SALES")
                .orElseThrow(() -> new RuntimeException("Vai trò SALES chưa tồn tại"));
        newUser.setRoles(Set.of(defaultRole));

        User saved = userRepository.save(newUser);
        saveAuditLog("REGISTER_GOOGLE", saved.getId(), "Đăng ký qua Google", ip, saved);
        return saved;
    }

    private void saveAuditLog(String action, Long entityId, String summary, String ip, User user) {
        AuditLog log = new AuditLog();
        log.setModule("AUTH");
        log.setAction(action);
        log.setEntityName("User");
        log.setEntityId(entityId);
        log.setSummary(summary);
        log.setIpAddress(ip);
        log.setPerformedBy(user);
        auditRepository.save(log);
    }
}