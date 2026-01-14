package com.sneakershop.backend.controller;

import com.sneakershop.backend.config.JwtTokenProvider;
import com.sneakershop.backend.dto.LoginRequest; // Thêm DTO mới
import com.sneakershop.backend.dto.UserRequest;
import com.sneakershop.backend.entity.Role;
import com.sneakershop.backend.entity.User;
import com.sneakershop.backend.service.AuthService;
import com.sneakershop.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid; // Để validate @NotBlank, @Size
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        User user = userService.findByUsername(request.getUsername());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Tài khoản hoặc mật khẩu không chính xác!");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Tài khoản đã bị khóa. Vui lòng liên hệ Admin!");
        }
        if (passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return generateAuthResponse(user);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Tài khoản hoặc mật khẩu không chính xác!");
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

    private ResponseEntity<?> generateAuthResponse(User user) {
        String token = tokenProvider.generateToken(user);

        String roleCode = "SALES";
        Set<Role> roles = user.getRoles();
        if (roles != null && !roles.isEmpty()) {
            roleCode = roles.iterator().next().getCode();
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("accessToken", token);
        responseData.put("username", user.getUsername());
        responseData.put("role", roleCode);

        return ResponseEntity.ok(responseData);
    }
}