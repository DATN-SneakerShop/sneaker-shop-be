package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.dto.voucher.CustomerVoucherDTO;
import com.sneakershop.backend.entity.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByUserId(Long userId);

    List<Customer> findByStatus(String status);

    List<Customer> findByStatusAndLoaiKhach(String status, String loaiKhach);

    List<Customer> findByStatusOrderByDiemTichLuyDesc(String status);

    List<Customer> findByLoaiKhach(String loaiKhach);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.status = 'ACTIVE' AND (
            LOWER(c.ten) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(c.email) LIKE LOWER(CONCAT('%', :kw, '%'))
            OR c.phone LIKE CONCAT('%', :kw, '%')
        )
    """)
    List<Customer> search(@Param("kw") String keyword);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.status = 'ACTIVE'
        AND c.updatedAt <= :date
    """)
    List<Customer> findInactiveCustomers(@Param("date") LocalDateTime date);

    @Query("SELECT new com.sneakershop.backend.dto.voucher.CustomerVoucherDTO(" +
            "c.id, c.ten, c.email, c.ngaySinh, c.loaiKhach, " +
            "COUNT(o.id), " +
            "COALESCE(SUM(o.totalAmount), 0), " +
            "c.createdAt, " +
            "MAX(o.createdAt)) " +
            "FROM Customer c LEFT JOIN Order o ON o.customer.id = c.id " +
            "GROUP BY c.id, c.ten, c.email, c.ngaySinh, c.loaiKhach, c.createdAt")
    List<CustomerVoucherDTO> findAllForVoucher();

    Optional<Customer> findByUser_Id(Long userId);

    @Query("select count(c) > 0 from Customer c where c.email is not null and lower(trim(c.email)) = lower(trim(:email))")
    boolean existsByEmailNormalized(@Param("email") String email);

    @Query("select count(c) > 0 from Customer c where c.email is not null and lower(trim(c.email)) = lower(trim(:email)) and c.id <> :id")
    boolean existsByEmailNormalizedAndIdNot(@Param("email") String email, @Param("id") Long id);

    @Query("select count(c) > 0 from Customer c where c.phone is not null and trim(c.phone) = trim(:phone)")
    boolean existsByPhoneNormalized(@Param("phone") String phone);

    @Query("select count(c) > 0 from Customer c where c.phone is not null and trim(c.phone) = trim(:phone) and c.id <> :id")
    boolean existsByPhoneNormalizedAndIdNot(@Param("phone") String phone, @Param("id") Long id);

}