package com.sneakershop.backend.repository.customer;

import com.sneakershop.backend.entity.customer.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    // Lấy danh sách địa chỉ theo ID khách, xếp cái mới nhất lên đầu
    List<CustomerAddress> findByCustomer_IdOrderByIdDesc(Long customerId);
}