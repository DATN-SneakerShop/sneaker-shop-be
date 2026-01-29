package com.sneakershop.backend.controller.login;

import com.sneakershop.backend.dto.login.UserRequest;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.service.login.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/management/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserManagementController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody UserRequest request, HttpServletRequest servletRequest) {
        try {
            // FIX: Lấy thông tin admin đang thực hiện thao tác
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User admin = userService.findByUsername(username);

            userService.createUser(request, servletRequest.getRemoteAddr(), admin);
            return ResponseEntity.ok("Thêm người dùng thành công");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRequest request, HttpServletRequest servletRequest) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User admin = userService.findByUsername(username);

            userService.updateUser(id, request, servletRequest.getRemoteAddr(), admin);
            return ResponseEntity.ok("Cập nhật thành công");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpServletRequest servletRequest) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User admin = userService.findByUsername(username);

            userService.deleteUser(id, servletRequest.getRemoteAddr(), admin);
            return ResponseEntity.ok("Xóa người dùng thành công");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}