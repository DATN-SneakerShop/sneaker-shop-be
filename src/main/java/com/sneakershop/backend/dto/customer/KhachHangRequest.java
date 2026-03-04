package com.sneakershop.backend.dto.customer;


import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Getter
@Setter
public class KhachHangRequest {

   @NotBlank
   private String hoTen;

    @Email
    private String email;


    private LocalDate ngaySinh;

  @NotBlank
   private String loaiKhach; // NORMAL | VIP
}
