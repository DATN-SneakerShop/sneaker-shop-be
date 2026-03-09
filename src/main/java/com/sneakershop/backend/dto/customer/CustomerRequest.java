package com.sneakershop.backend.dto.customer;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.*;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank(message="Tên không được trống")
    @Pattern(regexp="^[a-zA-ZÀ-ỹ\\s]+$",message="Tên không được chứa số hoặc ký tự đặc biệt")
    private String ten;


    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải nhỏ hơn ngày hiện tại")
    private LocalDate ngaySinh;

    @NotBlank(message = "Loại khách không được để trống")
    private String loaiKhach; // NORMAL | VIP

    @Min(value = 0, message = "Điểm không được âm")
    private Integer diemTichLuy;
}