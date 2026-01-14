package com.sneakershop.backend.controller;

import com.sneakershop.backend.dto.UserRequest;
import com.sneakershop.backend.entity.AuditLog;
import com.sneakershop.backend.entity.User;
import com.sneakershop.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/management/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserManagementController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequest request, HttpServletRequest servletRequest) {
        try {
            String ip = servletRequest.getRemoteAddr();
            userService.createUser(request, ip, null);
            return ResponseEntity.ok("Thêm người dùng thành công");
        } catch (IllegalArgumentException e) {
            // Trả về lỗi 400 kèm tin nhắn rõ ràng, không còn lỗi 500 nữa
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRequest request, HttpServletRequest servletRequest) {
        try {
            userService.updateUser(id, request, servletRequest.getRemoteAddr(), null);
            return ResponseEntity.ok("Cập nhật thành công");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest servletRequest) {
        try {
            userService.deleteUser(id, servletRequest.getRemoteAddr(), null);
            return ResponseEntity.ok("Xóa người dùng thành công");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(userService.getAllAuditLogs());
    }
}