package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // ✅ Hàm then chốt để check trùng Email
    boolean existsByEmail(String email);

    // Check trùng sđt
    boolean existsByPhone(String phone);

    List<Customer> findByStatus(String status);
    List<Customer> findByStatusAndLoaiKhach(String status, String loaiKhach);
    List<Customer> findByStatusOrderByDiemTichLuyDesc(String status);
    Optional<Customer> findByEmail(String email);
    List<Customer> findByLoaiKhach(String loaiKhach);

    // Tìm khách theo tên, sđt, email
    @Query("""
SELECT c FROM Customer c
WHERE c.status = 'ACTIVE' AND (
    LOWER(c.ten) LIKE LOWER(CONCAT('%', :kw, '%'))
    OR LOWER(c.email) LIKE LOWER(CONCAT('%', :kw, '%'))
    OR c.phone LIKE CONCAT('%', :kw, '%')
)
""")
    List<Customer> search(@Param("kw") String keyword);

    //Khách hàng lâu chưa hoạt động
    @Query("""
SELECT c FROM Customer c
WHERE c.status = 'ACTIVE'
AND c.updatedAt <= :date
""")
    List<Customer> findInactiveCustomers(@Param("date") LocalDateTime date);

}