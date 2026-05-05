package com.sneakershop.backend.service.login;

import com.sneakershop.backend.audit.AuditAction;
import com.sneakershop.backend.dto.login.UserRequest;
import com.sneakershop.backend.entity.login.AuditLog;
import com.sneakershop.backend.entity.login.Role;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.repository.login.AuditLogRepository;
import com.sneakershop.backend.repository.login.RoleRepository;
import com.sneakershop.backend.repository.login.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sneakershop.backend.entity.customer.Customer;
import com.sneakershop.backend.repository.customer.CustomerRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;


    public List<User> getAllUsers() {
        // 🔥 ĐỔI TỪ findAll() SANG findAllByOrderByIdDesc()
        return userRepository.findAllByOrderByIdDesc();
    }

    // 🔥 ĐÃ NÂNG CẤP CAMERA: Lấy cả Họ Tên và Danh sách Quyền
    @Transactional
    @AuditAction(module = "AUTH", action = "CREATE", entity = "User",
            description = "Đã cấp tài khoản: #{#request.username} | Tên: #{#request.fullName} | Email: #{#request.email} | Quyền: #{#request.roleCodes}")
    public void createUser(UserRequest request, String ip, User admin) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu tạo tài khoản không được để trống!");
        }
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống!");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống!");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống!");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên không được để trống!");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String fullName = request.getFullName().trim();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng!");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(fullName);
        user.setStatus("ACTIVE");
        user.setLoaiDangNhap("LOCAL");

        Set<Role> roles;
        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            roles = request.getRoleCodes().stream()
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .map(code -> roleRepository.findByCode(code)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quyền " + code)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        } else {
            Role customerRole = roleRepository.findByCode("CUSTOMER")
                    .orElseThrow(() -> new RuntimeException("Lỗi: Role CUSTOMER chưa được tạo!"));
            roles = Set.of(customerRole);
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);

        boolean isCustomer = roles.stream()
                .anyMatch(role -> "CUSTOMER".equalsIgnoreCase(role.getCode()));

        if (isCustomer) {
            if (customerRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("Email đã tồn tại trong danh sách khách hàng!");
            }

            Customer customer = new Customer();
            customer.setTen(fullName);
            customer.setEmail(email);
            customer.setPhone(null);
            customer.setNgaySinh(null);
            customer.setDiemTichLuy(0);
            customer.setLoaiKhach("BRONZE");
            customer.setStatus("ACTIVE");
            customer.setGhiChu("Tạo tự động từ tài khoản người dùng: " + username);
            customer.setUser(savedUser);

            customerRepository.save(customer);
        }
    }

    // 🔥 ĐÃ NÂNG CẤP CAMERA: Bắt sự thay đổi Họ tên, Email, Quyền
    @Transactional
    @AuditAction(module = "AUTH", action = "UPDATE", entity = "User",
            description = "Đã cập nhật tài khoản ID #{#id} | Tên: #{#request.fullName} | Email: #{#request.email} | Quyền: #{#request.roleCodes}")
    public void updateUser(Long id, UserRequest request, String ip, User admin) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email mới đã được sử dụng!");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoleCodes() != null) {
            Set<Role> roles = request.getRoleCodes().stream()
                    .map(code -> roleRepository.findByCode(code)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quyền " + code)))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }
        userRepository.save(user);
    }

    @Transactional
    @AuditAction(module = "AUTH", action = "DELETE", entity = "User",
            description = "Đã xóa vĩnh viễn tài khoản ID #{#userId} khỏi hệ thống")
    public void deleteUser(Long userId, String ip, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        List<AuditLog> logs = auditRepository.findByPerformedBy(user);
        for (AuditLog log : logs) {
            log.setPerformedBy(null);
        }
        auditRepository.saveAll(logs);
        userRepository.delete(user);
    }

    // 🔥 NÂNG CẤP CAMERA: Lấy tên và email mới khi User tự cập nhật Profile
    @Transactional
    @AuditAction(module = "AUTH", action = "UPDATE_PROFILE", entity = "User",
            description = "Đã tự cập nhật hồ sơ cá nhân | Tên mới: #{#fullName} | Email mới: #{#email}")
    public void updateProfile(String username, String fullName, String email, String ip) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        user.setFullName(fullName);
        user.setEmail(email);
        userRepository.save(user);
    }

    @Transactional
    @AuditAction(module = "AUTH", action = "CHANGE_PASSWORD", entity = "User",
            description = "Đã chủ động thay đổi mật khẩu tài khoản")
    public void changePassword(String username, String oldPwd, String newPwd, String ip) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(oldPwd, user.getPasswordHash())) throw new IllegalArgumentException("Mật khẩu cũ sai!");
        user.setPasswordHash(passwordEncoder.encode(newPwd));
        userRepository.save(user);
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditRepository.findAllByOrderByCreatedAtDesc();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}