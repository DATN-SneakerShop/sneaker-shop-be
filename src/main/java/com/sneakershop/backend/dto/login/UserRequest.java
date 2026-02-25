package com.sneakershop.backend.dto.login;

import lombok.Data;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class UserRequest {
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    // Password dùng chung cho cả Register và Reset Password
    private String password;

    private List<String> roleCodes;

    // Trường mới để nhận mã OTP khi Reset Password
    private String otp;
}