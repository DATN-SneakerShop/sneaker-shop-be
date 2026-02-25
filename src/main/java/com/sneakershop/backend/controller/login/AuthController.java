package com.sneakershop.backend.controller.login;

import com.sneakershop.backend.config.JwtTokenProvider;
import com.sneakershop.backend.dto.login.LoginRequest;
import com.sneakershop.backend.dto.login.UserRequest;
import com.sneakershop.backend.entity.login.Role;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.repository.login.UserRepository;
import com.sneakershop.backend.service.login.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Đăng nhập bằng Email (Trường username trong request giờ chứa Email)
        User user = userRepository.findByEmail(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Email hoặc mật khẩu không chính xác!");
        }
        return generateAuthResponse(user);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            authService.generateOtpForPasswordReset(body.get("email"));
            return ResponseEntity.ok("Mã OTP đã được gửi về Gmail của bạn!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody UserRequest request, HttpServletRequest servletRequest) {
        try {
            authService.verifyAndResetPassword(
                    request.getEmail(),
                    request.getOtp(),
                    request.getPassword(),
                    servletRequest.getRemoteAddr()
            );
            return ResponseEntity.ok("Đặt lại mật khẩu thành công! Hãy đăng nhập bằng mật khẩu mới.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRequest request, HttpServletRequest servletRequest) {
        try {
            authService.registerLocal(request, servletRequest.getRemoteAddr());
            return ResponseEntity.ok("Đăng ký thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ResponseEntity<?> generateAuthResponse(User user) {
        String token = tokenProvider.generateToken(user);
        List<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toList());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("accessToken", token);
        responseData.put("email", user.getEmail());
        responseData.put("fullName", user.getFullName());
        responseData.put("roles", roles);

        return ResponseEntity.ok(responseData);
    }
}