package com.sneakershop.backend.service.login;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.sneakershop.backend.audit.SystemAuditLogService;
import com.sneakershop.backend.dto.login.UserRequest;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.entity.login.*;
import com.sneakershop.backend.repository.customer.CustomerRepository;
import com.sneakershop.backend.repository.login.*;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    private final CustomerRepository customerRepository;

    // 🔥 Gọi Service Log Tổng mới thay cho Repo Log cũ

    private final SystemAuditLogService systemAuditLogService;

    // 🔥 HÀM MỚI: XỬ LÝ ĐĂNG NHẬP BẰNG GOOGLE
    @Transactional
    public User loginWithGoogle(String idTokenString, String ip) throws Exception {
        // Khởi tạo bộ xác thực của Google
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                // ⚠️ THAY BẰNG GOOGLE CLIENT ID CỦA MÀY VÀO ĐÂY
                .setAudience(Collections.singletonList("1090844427851-g1hfpluc79irjnftmvn620m8g261rfct.apps.googleusercontent.com"))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            User user = userRepository.findByEmail(email).orElse(null);

            // Nếu User chưa tồn tại -> Tự động tạo tài khoản mới
            if (user == null) {
                user = new User();
                user.setUsername(email);
                user.setEmail(email);
                user.setFullName(name);
                user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString())); // Pass ngẫu nhiên
                user.setLoaiDangNhap("GOOGLE");
                user.setStatus("ACTIVE");

                Role customerRole = roleRepository.findByCode("CUSTOMER")
                        .orElseThrow(() -> new RuntimeException("Role CUSTOMER chưa có trong hệ thống!"));
                user.setRoles(Set.of(customerRole));

                userRepository.save(user);

                systemAuditLogService.logManual(email, ip, "AUTH", "REGISTER_GOOGLE", "User", "Đăng ký mới qua Google", "SUCCESS", null, "INFO");
            }

            systemAuditLogService.logManual(email, ip, "AUTH", "LOGIN_GOOGLE", "User", "Đăng nhập bằng Google", "SUCCESS", null, "INFO");
            return user;
        } else {
            throw new IllegalArgumentException("Token Google không hợp lệ hoặc đã hết hạn!");
        }
    }

    @Transactional
    public void registerLocal(UserRequest request, String ip) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email này đã được đăng ký!");
        }

        User user = new User();
        user.setUsername(request.getEmail());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setLoaiDangNhap("LOCAL");
        user.setStatus("ACTIVE");

        Role customerRole = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Role CUSTOMER chưa có trong hệ thống!"));
        user.setRoles(Set.of(customerRole));

        userRepository.save(user);


        // Khi tạo tài khoản sẽ có trong danh sách khách hàng
        Customer customer = new Customer();

        customer.setTen(user.getFullName());
        customer.setEmail(user.getEmail());
        customer.setDiemTichLuy(0);

        customer.setLoaiKhach("NORMAL");
        customerRepository.save(customer);

        // ✅ GHI LOG THỦ CÔNG QUA SERVICE MỚI
        systemAuditLogService.logManual(
                request.getEmail(), ip, "AUTH", "REGISTER", "User",
                "Đăng ký mới qua Email: " + user.getEmail(), "SUCCESS", null, "INFO"
        );
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

        systemAuditLogService.logManual(
                email, "SYSTEM", "AUTH", "REQUEST_OTP", "User",
                "Yêu cầu mã OTP khôi phục mật khẩu", "SUCCESS", null, "INFO"
        );
    }

    @Transactional
    public void verifyAndResetPassword(String email, String otp, String newPassword, String ip) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại!"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(otp)) {
            throw new IllegalArgumentException("Mã OTP không chính xác!");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn, vui lòng lấy mã mới!");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setOtpCode(null);
        user.setOtpExpiry(null);

        userRepository.save(user);

        systemAuditLogService.logManual(
                email, ip, "AUTH", "RESET_PASSWORD", "User",
                "Đổi mật khẩu thành công qua OTP", "SUCCESS", null, "INFO"
        );
    }

    private void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}