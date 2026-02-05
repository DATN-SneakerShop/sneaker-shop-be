package com.sneakershop.backend.service.login;

import com.sneakershop.backend.dto.login.UserRequest;
import com.sneakershop.backend.entity.login.*;
import com.sneakershop.backend.repository.login.*;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public void registerLocal(UserRequest request, String ip) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email này đã được đăng ký!");
        }

        User user = new User();
        user.setUsername(request.getEmail()); // Dùng email làm username
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setLoaiDangNhap("LOCAL");
        user.setStatus("ACTIVE");

        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER chưa có trong hệ thống!"));
        user.setRoles(Set.of(customerRole));

        User savedUser = userRepository.save(user);
        saveAuditLog("REGISTER", savedUser.getId(), "Đăng ký mới qua Email: " + user.getEmail(), ip, savedUser);
    }

    @Transactional
    public void generateOtpForPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với email này!"));

        String otp = String.format("%06d", new Random().nextInt(1000000));
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        sendEmail(email, "Mã xác thực OTP SneakerShop", "Mã của bạn là: " + otp + " (Hiệu lực trong 5 phút)");
        saveAuditLog("REQUEST_OTP", user.getId(), "Yêu cầu mã OTP khôi phục mật khẩu", "SYSTEM", user);
    }

    // --- LOGIC MỚI: XÁC THỰC VÀ ĐỔI MẬT KHẨU ---
    @Transactional
    public void verifyAndResetPassword(String email, String otp, String newPassword, String ip) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại!"));

        // 1. Kiểm tra OTP
        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new IllegalArgumentException("Mã OTP không chính xác!");
        }

        // 2. Kiểm tra thời gian hết hạn
        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn, vui lòng lấy mã mới!");
        }

        // 3. Cập nhật Pass mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // 4. Xóa OTP sau khi dùng xong
        user.setOtpCode(null);
        user.setOtpExpiry(null);

        userRepository.save(user);

        // 5. Lưu vết hệ thống
        saveAuditLog("RESET_PASSWORD", user.getId(), "Đổi mật khẩu thành công qua OTP", ip, user);
    }

    private void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
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