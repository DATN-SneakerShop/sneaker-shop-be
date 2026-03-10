package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // ✅ Hàm then chốt để check trùng Email
    boolean existsByEmail(String email);

    List<Customer> findByStatus(String status);
    List<Customer> findByStatusAndLoaiKhach(String status, String loaiKhach);
    List<Customer> findByStatusOrderByDiemTichLuyDesc(String status);
    Optional<Customer> findByEmail(String email);


}