package com.sneakershop.backend.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class UserRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 5, max = 20, message = "Tên đăng nhập phải từ 5-20 ký tự")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    // Không dùng @NotBlank ở đây để khi Update không bắt buộc nhập mật khẩu mới
    private String password;

    // Trường này cực kỳ quan trọng để fix lỗi build
    private List<String> roleCodes;
}