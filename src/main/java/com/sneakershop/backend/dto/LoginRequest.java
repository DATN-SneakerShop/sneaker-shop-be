package com.sneakershop.backend.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 5, message = "Tên đăng nhập phải có ít nhất 5 ký tự")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải từ 6 ký tự trở lên")
    private String password;
}