package com.sneakershop.backend.config;

import com.sneakershop.backend.entity.login.Role;
import com.sneakershop.backend.entity.login.User;
import com.sneakershop.backend.repository.RoleRepository;
import com.sneakershop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Khởi tạo danh sách các Vai trò (Roles)
        if (roleRepository.count() == 0) {
            List<Role> defaultRoles = List.of(
                    new Role("ADMIN", "Quản trị viên"),
                    new Role("SALES", "Nhân viên bán hàng"),
                    new Role("INVENTORY", "Thủ kho"),
                    new Role("CUSTOMER", "Khách hàng")
            );
            roleRepository.saveAll(defaultRoles);
            System.out.println(">>> Đã khởi tạo danh sách quyền thành công!");
        }

        // 2. Tạo tài khoản Admin mặc định để có thể đăng nhập ngay
        if (userRepository.count() == 0) {
            Role adminRole = roleRepository.findByCode("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Lỗi: Không tìm thấy quyền ADMIN"));

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@sneakershop.com");
            // Mật khẩu mặc định là: 123456
            admin.setPasswordHash(passwordEncoder.encode("123456"));
            admin.setFullName("Hệ Thống Admin");
            admin.setStatus("ACTIVE");
            admin.setRoles(Set.of(adminRole));

            userRepository.save(admin);
            System.out.println(">>> Đã tạo tài khoản Admin mặc định (User: admin / Pass: 123456)");
        }
    }
}