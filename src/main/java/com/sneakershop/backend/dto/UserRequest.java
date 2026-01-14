package com.sneakershop.backend.dto;

import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class UserRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 5, max = 20, message = "Tên đăng nhập phải từ 5-20 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Tên đăng nhập không được chứa ký tự đặc biệt")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải từ 6 ký tự trở lên")
    private String password;

    private List<String> roleCodes;
}