package com.sneakershop.backend.controller.login;

import com.sneakershop.backend.config.JwtTokenProvider;
import com.sneakershop.backend.dto.login.LoginRequest;
import com.sneakershop.backend.dto.login.UserRequest;
import com.sneakershop.backend.entity.login.Role;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.service.login.AuthService;
import com.sneakershop.backend.service.login.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userService.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Sai tài khoản hoặc mật khẩu!");
        }
        return generateAuthResponse(user); // Dùng chung hàm để chuẩn hóa dữ liệu trả về
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByUsername(username);
        if (user == null) return ResponseEntity.status(404).body("User not found");

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("roles", user.getRoles().stream().map(Role::getCode).collect(Collectors.toList()));
        return ResponseEntity.ok(data);
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> data, HttpServletRequest request) {
        try {
            String googleId = data.get("googleId");
            String email = data.get("email");
            String name = data.get("name");
            String ip = request.getRemoteAddr();

            if (googleId == null || email == null) {
                return ResponseEntity.badRequest().body("Dữ liệu Google không hợp lệ!");
            }

            User user = authService.processGoogleAuth(googleId, email, name, ip);
            return generateAuthResponse(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
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

    // FIX: Chuẩn hóa trả về roles cho cả Login nội bộ và Google
    private ResponseEntity<?> generateAuthResponse(User user) {
        String token = tokenProvider.generateToken(user);
        List<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toList());

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("accessToken", token);
        responseData.put("username", user.getUsername());
        responseData.put("fullName", user.getFullName());
        responseData.put("roles", roles); // Luôn trả về mảng

        return ResponseEntity.ok(responseData);
    }
}