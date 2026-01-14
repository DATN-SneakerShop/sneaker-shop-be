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
        // 1. Tìm xem email này đã tồn tại trong hệ thống chưa
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            // TRƯỜNG HỢP 1: Tài khoản đã tồn tại

            // CẬP NHẬT QUAN TRỌNG: Nếu tài khoản đang ở trạng thái DELETED, hãy khôi phục lại
            if ("DELETED".equals(user.getStatus())) {
                user.setStatus("ACTIVE");
            }

            // Cập nhật thông tin GoogleId nếu trước đó chưa có (trường hợp user local chuyển sang login google)
            user.setGoogleId(googleId);
            user.setFullName(name); // Cập nhật lại tên từ Google cho mới nhất
        } else {
            // TRƯỜNG HỢP 2: Tài khoản mới hoàn toàn
            user = new User();
            // Lấy phần trước chữ @ của email làm username mặc định
            String defaultUsername = email.split("@")[0];

            // Đảm bảo username không bị trùng nếu đã có người dùng khác trùng tên
            if (userRepository.existsByUsername(defaultUsername)) {
                user.setUsername(email); // Nếu trùng thì dùng cả email làm username
            } else {
                user.setUsername(defaultUsername);
            }

            user.setEmail(email);
            user.setFullName(name);
            user.setGoogleId(googleId);
            user.setStatus("ACTIVE");
            user.setLoaiDangNhap("GOOGLE");

            // Mật khẩu cho user Google nên để một giá trị ngẫu nhiên hoặc mặc định vì họ đăng nhập qua Google
            user.setPasswordHash(passwordEncoder.encode("GOOGLE_AUTH_SECURE_" + googleId));

            // Gán Role mặc định (Ví dụ: SALES hoặc CUSTOMER)
            Role defaultRole = roleRepository.findByCode("SALES")
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quyền mặc định"));
            user.getRoles().add(defaultRole);
        }

        // Lưu lại (Update hoặc Insert)
        User savedUser = userRepository.save(user);

        // Ghi log đăng nhập
        saveAuditLog("GOOGLE_LOGIN", savedUser.getId(), "Đăng nhập bằng Google: " + email, ip, savedUser);

        return savedUser;
    }

    // Hàm ghi log phụ trợ (nếu chưa có trong AuthService)
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