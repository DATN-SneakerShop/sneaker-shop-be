package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByStatus(String status);

    List<Customer> findByStatusAndLoaiKhach(String status, String loaiKhach);

    List<Customer> findByStatusOrderByDiemTichLuyDesc(String status);

    List<Customer> findByStatusAndLoaiKhachOrderByDiemTichLuyDesc(
            String status,
            String loaiKhach
    );
}
