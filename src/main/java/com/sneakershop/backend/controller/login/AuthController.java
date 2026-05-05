package com.sneakershop.backend.controller.login;

import com.sneakershop.backend.audit.SystemAuditLogService;
import com.sneakershop.backend.config.JwtTokenProvider;
import com.sneakershop.backend.dto.login.CurrentAccountResponse;
import com.sneakershop.backend.dto.login.CurrentAddressResponse;
import com.sneakershop.backend.dto.login.CurrentCustomerResponse;
import com.sneakershop.backend.dto.login.LoginRequest;
import com.sneakershop.backend.dto.login.UpdateCurrentCustomerRequest;
import com.sneakershop.backend.dto.login.UpsertCurrentAddressRequest;
import com.sneakershop.backend.dto.login.UserRequest;
import com.sneakershop.backend.entity.login.Role;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.repository.login.UserRepository;
import com.sneakershop.backend.service.login.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;
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
    private final SystemAuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String ip = servletRequest.getRemoteAddr();
        String attemptedEmail = request.getUsername() != null ? request.getUsername() : "UNKNOWN";

        User user = userRepository.findByEmail(attemptedEmail).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditLogService.logManual(
                    attemptedEmail, ip, "SECURITY", "LOGIN_FAILED", "User",
                    "Cảnh báo: Đăng nhập thất bại (Sai email hoặc mật khẩu)",
                    "FAILED", "Sai thông tin đăng nhập", "WARNING"
            );
            return ResponseEntity.status(401).body("Email hoặc mật khẩu không chính xác!");
        }

        auditLogService.logManual(
                user.getEmail(), ip, "AUTH", "LOGIN_SUCCESS", "User",
                "Đăng nhập hệ thống thành công",
                "SUCCESS", null, "INFO"
        );

        return generateAuthResponse(user);
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String credential = body.get("credential");
            User user = authService.loginWithGoogle(credential, request.getRemoteAddr());
            return generateAuthResponse(user);
        } catch (Exception e) {
            auditLogService.logManual(
                    "GUEST",
                    request.getRemoteAddr(),
                    "SECURITY",
                    "LOGIN_FAILED",
                    "User",
                    "Đăng nhập Google thất bại",
                    "FAILED",
                    e.getMessage(),
                    "WARNING"
            );
            return ResponseEntity.status(401).body("Xác thực Google thất bại: " + e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentAccount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn chưa đăng nhập");
        }

        CurrentAccountResponse response = authService.getCurrentAccount(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/customer")
    public ResponseEntity<?> updateCurrentCustomer(
            Principal principal,
            @RequestBody UpdateCurrentCustomerRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn chưa đăng nhập");
        }

        CurrentCustomerResponse response = authService.updateCurrentCustomer(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/addresses")
    public ResponseEntity<?> getCurrentAddresses(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn chưa đăng nhập");
        }

        List<CurrentAddressResponse> response = authService.getCurrentAddresses(principal.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<?> createCurrentAddress(
            Principal principal,
            @RequestBody UpsertCurrentAddressRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn chưa đăng nhập");
        }

        CurrentAddressResponse response = authService.createCurrentAddress(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/addresses/{addressId}")
    public ResponseEntity<?> updateCurrentAddress(
            Principal principal,
            @PathVariable Long addressId,
            @RequestBody UpsertCurrentAddressRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn chưa đăng nhập");
        }

        CurrentAddressResponse response = authService.updateCurrentAddress(principal.getName(), addressId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<?> deleteCurrentAddress(Principal principal, @PathVariable Long addressId) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn chưa đăng nhập");
        }

        authService.deleteCurrentAddress(principal.getName(), addressId);
        return ResponseEntity.ok("Xóa địa chỉ thành công");
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